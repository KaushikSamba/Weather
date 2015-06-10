package com.kaushiksamba.weather;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;


public class MainActivity extends ActionBarActivity{
    String weather_data,address,location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*        int x = get_current_location();
        if(x==1)
        {
            begin();
        }
  */
        if(get_current_location()==1) begin();
    }

    protected int get_current_location()
    {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            final Location last_known_location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addressList = geocoder.getFromLocation(last_known_location.getLatitude(),last_known_location.getLongitude(),1);
                location = addressList.get(0).getLocality();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 1;
        }
            else
            {
                Toast.makeText(this,"No network, please try again later. :(",Toast.LENGTH_SHORT).show();
                return 0;
            }
    }

    public void begin()
    {
        String address_start = "http://api.openweathermap.org/data/2.5/weather?q=";
        String address_close = "&mode=xml";
        address = address_start+location+address_close;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    weather_data = scrape(address);
                    Log.d("Weather data","\n"+weather_data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (weather_data.contains("Not found city"))
                {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "City not found", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else
                {
                    try {
                        parse();
                    } catch (XmlPullParserException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    private String get_city_name(XmlPullParser xmlPullParser) throws IOException, XmlPullParserException
    {
        while (!xmlPullParser.getName().equals("city")) xmlPullParser.nextTag();
        return xmlPullParser.getAttributeValue(null,"name");
    }

    private String get_country_name(XmlPullParser xmlPullParser) throws IOException, XmlPullParserException {
        while(!xmlPullParser.getName().equals("country")) xmlPullParser.nextTag();
        xmlPullParser.next();
        String country_name = xmlPullParser.getText();
        switch(country_name)
        {
            case "IN":  country_name="India";
                        break;
            case "AR":  country_name="Argentina";
                        break;
            case "GB":  country_name="Great Britain";
                        break;
            case "CO":  country_name="Colombia";
                        break;
            case "SG":  country_name="Singapore";
                        break;
        }
        xmlPullParser.nextTag();
        return country_name;
    }

    private String K_to_C(String value)
    {
        float temp = Float.valueOf(value);
        temp-=273.15;
        float absval = ((int) temp);
        if(temp-absval>0.5) temp = absval + 1;
            else temp = absval;
        return Integer.toString(((int) temp));
    }
    private String get_max_temp(XmlPullParser xmlPullParser)
    {
        String temp = xmlPullParser.getAttributeValue(null, "max");
        temp = K_to_C(temp);
        return temp;
    }

    private String get_min_temp(XmlPullParser xmlPullParser)
    {
        String temp = xmlPullParser.getAttributeValue(null,"min");
        temp = K_to_C(temp);
        return temp;
    }
    private void get_temperatures(XmlPullParser xmlPullParser) throws IOException, XmlPullParserException
    {
        while(!xmlPullParser.getName().equals("temperature")) xmlPullParser.nextTag();
    }

    private String get_weather_value(XmlPullParser xmlPullParser) throws IOException, XmlPullParserException
    {
        while(!xmlPullParser.getName().equals("weather")) xmlPullParser.nextTag();
        return xmlPullParser.getAttributeValue(null,"value");
    }
    protected void parse() throws XmlPullParserException, IOException
    {
        final XmlPullParser xmlPullParser = Xml.newPullParser();
        xmlPullParser.setInput(stringToInputStream(weather_data), "UTF-8");
        xmlPullParser.nextTag();
        final String city_name = get_city_name(xmlPullParser);
        final String country_name = get_country_name(xmlPullParser);
        get_temperatures(xmlPullParser);
        final String max_temp = get_max_temp(xmlPullParser);
        final String min_temp = get_min_temp(xmlPullParser);
        final String weather_value = get_weather_value(xmlPullParser);
        final Bundle bundle = new Bundle();
        bundle.putString("city_name", city_name);
        bundle.putString("country_name", country_name);
        bundle.putString("max_temp", max_temp);
        bundle.putString("min_temp", min_temp);
        bundle.putString("weather_value", weather_value);
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateScreen(bundle);
            }
        });
    }
    
    protected void updateScreen(Bundle bundle)
    {
        String city_name = bundle.getString("city_name");
            city_name = city_name.toUpperCase();
        String country_name = bundle.getString("country_name");
            country_name = country_name.toUpperCase();

        String max_temp = bundle.getString("max_temp");
        String min_temp = bundle.getString("min_temp");

        String weather_value = bundle.getString("weather_value");
            weather_value = weather_value.toLowerCase();
            weather_value = weather_value.replace(' ','_');

        ImageView imageView = (ImageView) findViewById(R.id.icon);
        int id = getResources().getIdentifier(weather_value,"drawable",getPackageName());
        if(id==0) imageView.setImageResource(R.drawable.weather);
            else imageView.setImageResource(id);

        TextView city = (TextView) findViewById(R.id.city_name);
            city.setText(city_name);
        TextView country = (TextView) findViewById(R.id.country_name);
            country.setText(country_name);
        TextView max = (TextView) findViewById(R.id.max_temp);
            max.setText(max_temp);
        TextView min = (TextView) findViewById(R.id.min_temp);
            min.setText(min_temp);
    }
    protected InputStream stringToInputStream(String text)
    {
        return new ByteArrayInputStream(text.getBytes(Charset.forName("UTF-8")));
    }

    protected String scrape(String address) throws IOException
    {
        URL url = new URL(address);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        final int rc = httpURLConnection.getResponseCode();

        if(rc==200)
        {
            StringBuilder data = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            String one_line;
            do {
                one_line = bufferedReader.readLine();
                data.append(one_line).append("\n");
            }while (one_line!=null);
            bufferedReader.close();
            httpURLConnection.disconnect();
            return data.toString();
        }
        return null;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //return true;)
            Intent intent = new Intent(this,MenuActivity.class);
            startActivityForResult(intent, 1);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(data!=null) location = data.getStringExtra("Location");
        begin();
    }

}
