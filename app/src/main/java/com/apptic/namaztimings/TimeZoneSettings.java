package com.apptic.namaztimings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class TimeZoneSettings extends AppCompatActivity {

    private AppCompatSpinner timeZoneSpinner;
    private ImageView backIcon;
    private TextView selected_time_zone_txt;

    private EditText searchbox;
    private SharedPreferences sharedPreferences;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        searchbox = findViewById(R.id.searchbox);
        timeZoneSpinner = findViewById(R.id.timeZoneSpinner);
        backIcon = findViewById(R.id.backIcon);
        selected_time_zone_txt = findViewById(R.id.selected_time_zone_txt);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Set an initial text for the selected_time_zone_txt TextView based on the saved timezone
        String savedTimeZone = sharedPreferences.getString("SelectedTimeZone", "");
        if (!savedTimeZone.isEmpty()) {
            selected_time_zone_txt.setText(savedTimeZone);
        } else {
            selected_time_zone_txt.setText("none");
        }

        // Add an OnClickListener to the selected_time_zone_txt TextView
        selected_time_zone_txt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the timezone spinner
                timeZoneSpinner.performClick();
            }
        });

        // Add an OnClickListener to the selected_time_zone_txt TextView
        searchbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the timezone spinner
                selected_time_zone_txt.performClick();
            }
        });


        // Extract and set the list of time zones defined in coordinates.xml
        List<String> definedTimeZones = extractDefinedTimeZones();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, definedTimeZones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the adapter for the Spinner
        timeZoneSpinner.setAdapter(adapter);

        // Check saved time zone preference and set the default if it's empty
        if (!savedTimeZone.isEmpty()) {
            int savedTimeZoneIndex = adapter.getPosition(savedTimeZone);
            if (savedTimeZoneIndex >= 0) {
                timeZoneSpinner.setSelection(savedTimeZoneIndex);
            } else {
                // Handle the case where the saved timezone is not in the definedTimeZones list.
                // You may choose to set a default selection or handle it differently.
            }
        } else {
            // Default to the first time zone if the preference is empty
            timeZoneSpinner.setSelection(0);
        }

        // Add a TextWatcher to the searchbox EditText
        searchbox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Not needed for this implementation
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Handle text changes here
                String searchText = charSequence.toString().toLowerCase();
                List<String> filteredTimeZones = new ArrayList<>();

                for (String timeZone : definedTimeZones) {
                    if (timeZone.toLowerCase().contains(searchText)) {
                        filteredTimeZones.add(timeZone);
                    }
                }

                // Update the adapter with the filtered list
                adapter.clear();
                adapter.addAll(filteredTimeZones);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Not needed for this implementation
            }
        });

        // Handle item selection in the Spinner
        timeZoneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Get the selected time zone
                String selectedTimeZone = (String) timeZoneSpinner.getSelectedItem();

                // Save the selected time zone to SharedPreferences
                saveSelectedTimeZone(selectedTimeZone);

                // Extract coordinates from the XML file
                extractCoordinates(selectedTimeZone);

                // Update the text in the selected_time_zone_txt TextView
                if (selectedTimeZone != null && !selectedTimeZone.isEmpty()) {
                    selected_time_zone_txt.setText(selectedTimeZone);
                } else {
                    selected_time_zone_txt.setText("none");
                }

                // Broadcast an intent to inform the MainActivity about the time zone change
                Intent intent = new Intent("com.apptic.namaztimings.TIME_ZONE_CHANGED");
                sendBroadcast(intent);

                // Show a toast to confirm the selection
                // Toast.makeText(TimeZoneSettings.this, "Selected Time Zone: " + selectedTimeZone, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Handle when nothing is selected, if needed.
            }
        });

        // Set an OnClickListener for the backIcon
        backIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to the previous activity
                finish();
            }
        });
    }

    private List<String> extractDefinedTimeZones() {
        List<String> definedTimeZones = new ArrayList<>();
        try {
            // Load the XML file
            InputStream inputStream = getResources().openRawResource(R.raw.coordinates);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();

            // Find the location elements and extract the time zones
            NodeList locationList = doc.getElementsByTagName("location");
            for (int i = 0; i < locationList.getLength(); i++) {
                Node node = locationList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String timeZone = element.getElementsByTagName("name").item(0).getTextContent();
                    definedTimeZones.add(timeZone);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return definedTimeZones;
    }

    private void saveSelectedTimeZone(String timeZone) {
        // Use SharedPreferences to save the selected time zone
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("SelectedTimeZone", timeZone);
        editor.apply();
    }

    private void extractCoordinates(String selectedTimeZone) {
        try {
            // Load the XML file
            InputStream inputStream = getResources().openRawResource(R.raw.coordinates);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();

            // Find the location element with the specified name
            NodeList locationList = doc.getElementsByTagName("location");
            for (int i = 0; i < locationList.getLength(); i++) {
                Node node = locationList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String name = element.getElementsByTagName("name").item(0).getTextContent();
                    if (name.equals(selectedTimeZone)) {
                        String latitude = element.getElementsByTagName("latitude").item(0).getTextContent();
                        String longitude = element.getElementsByTagName("longitude").item(0).getTextContent();

                        // Store latitude and longitude for use in other activities
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("SelectedLatitude", latitude);
                        editor.putString("SelectedLongitude", longitude);
                        editor.apply();

                        break; // Exit the loop after finding the location
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
