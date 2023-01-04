package iss.workshop.simplememorygame;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    //Back Ground Thread
    private Thread bgThread;

    //Array List of String Type to Store Selected Images Absolute Link
    ArrayList<String> selectedImageList = new ArrayList<String>();

    //Array List to store selected Image View IDs
    List<Integer> pickedImageViewsList = new ArrayList<Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Resources r = getResources();
        String name = getPackageName();

        // Fetch Button Implementation to Load Images of 280 * 420 size
        findViewById(R.id.btnFetch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = findViewById(R.id.inputTxt);
                ProgressBar progressBar = findViewById(R.id.progressBar);
                TextView progressText = findViewById(R.id.progressText);

                String inputURL = "https://" + editText.getText().toString();
                progressBar.setProgress(0);
                progressText.setText("Download starting..");
                clearSelection();

                if (bgThread != null) {
                    bgThread.interrupt();
                    for (int i = 1; i <= 20; i++) {
                        ImageView img = findViewById(r.getIdentifier("image" + i, "id", name));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                img.setImageResource(R.drawable.cross);
                            }
                        });
                    }
                }

                bgThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //Using JSoup to Parse Images Tags from HTML Document
                            Document htmlDoc = Jsoup.connect(inputURL).get();
                            Elements selectedElement = htmlDoc.getElementsByTag("img");
                            //Log.d("Testing at Line 72","Element Count" +selectedElement.size());

                            //Element Parsing to filter images properties
                            List<String> urlsList = parsingDocumentElement(selectedElement);

                            //Iterate the URL Collected List to set bitmap images to Image View
                            for (int i = 1; i <= urlsList.size(); i++) {
                                if (bgThread.interrupted())
                                    return;
                                URL url = new URL(urlsList.get(i - 1));

                                //Downloaded Image as Bitmap
                                Bitmap bmpimg = ImageDownload.downloadImg(urlsList, i);

                                //Simple naming convention of ImageView to allow for looping
                                ImageView img = findViewById(r.getIdentifier("image" + i, "id", name));

                                //UI thread started to allow setting of images and progress bar/text
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        img.setImageBitmap(bmpimg);
                                        img.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                imageSelect(url, img.getId());
                                            }
                                        });
                                        progressBar.incrementProgressBy(1);
                                        progressText.setText("Downloading " + progressBar.getProgress() + " of 20 images");
                                        if (progressBar.getProgress() == 20) {
                                            progressText.setText("Download completed");
                                        }
                                    }
                                });
                                Thread.sleep(200);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                bgThread.start();

            }
        });

        // Start the Game Flow --passing selected 6 images
        findViewById(R.id.btnSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedImageList.size() == 6) {
                    Log.d("Starting Intent","To Start Game");

                    Intent intent = new Intent(MainActivity.this, GameActivity.class);
                    Log.d("Intenet","List Size "+selectedImageList.size());
                    intent.putExtra("selectedimgList", selectedImageList);
                    startActivity(intent);

                    System.out.println("intent okay");
                    Log.d("Ending Intent","To Start Game");
                }
            }
        });
    }


    private List<String> parsingDocumentElement(Elements selectedElement) {
        List<String> stringList=new ArrayList<>();
        for (Element imageElement : selectedElement) {
            String source="";
            Integer height=0;
            Integer width=0;

            if(imageElement.attr("src")!=null && imageElement.attr("height").length()>1)
                source = imageElement.attr("src");
            if(imageElement.attr("height")!=null && imageElement.attr("height").length()>1)
                height = Integer.parseInt(imageElement.attr("height"));
            if(imageElement.attr("width")!=null && imageElement.attr("width").length()>1)
                width = Integer.parseInt(imageElement.attr("width"));

            if(!imageElement.absUrl("src").endsWith("svg") && height==280 && width==420 && stringList.size() <20) {
                stringList.add(imageElement.attr("src"));
            }
        }
        return stringList;
    }

    // Method for Selected Image to add in lists & perform validation
    protected void imageSelect(URL url, int id) {
        if (selectedImageList.size() < 6) {
            selectedImageList.add(url.toString());
            pickedImageViewsList.add(id);
            // findViewById(id).setBackground(MainActivity.this.getDrawable(R.drawable.button_border));
            if (selectedImageList.size() == 6) {
                findViewById(R.id.btnSubmit).setEnabled(true);
            }
        }
        else
        {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Select only 6 images")
                    .setMessage("Do you want to select again?")

                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            clearSelection();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    // Clear selection stored in list and to reset view
    protected void clearSelection()
    {
        for (Integer i: pickedImageViewsList)
        {
            findViewById(i).setBackground(null);
        }
        pickedImageViewsList.clear();
        selectedImageList.clear();
        findViewById(R.id.btnSubmit).setEnabled(false);
    }
}