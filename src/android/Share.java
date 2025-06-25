package nl.madebymark.share;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class echoes a string called from JavaScript.
 */
public class Share extends CordovaPlugin {    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("share")) {
            String message = args.getString(0);
            String subject = args.getString(1);
            JSONArray toArray = args.getJSONArray(2);
            JSONArray filesArray = args.getJSONArray(3);
            
            // Convert JSONArray to String array
            String[] toEmails = new String[toArray.length()];
            for (int i = 0; i < toArray.length(); i++) {
                toEmails[i] = toArray.getString(i);
            }
            
            this.share(message, subject, toEmails, filesArray, callbackContext);
            return true;
        }
        return false;
    }    private void share(String message, String subject, String[] to, JSONArray filesArray, CallbackContext callbackContext) {
        try {
            Intent emailIntent;
            ArrayList<Uri> attachmentUris = new ArrayList<>();
            
            // Process file attachments
            if (filesArray != null && filesArray.length() > 0) {
                for (int i = 0; i < filesArray.length(); i++) {
                    JSONObject fileObj = filesArray.getJSONObject(i);
                    String fileName = fileObj.getString("fileName");
                    String base64Data = fileObj.getString("base64");
                    String mimeType = fileObj.optString("mimeType", "application/octet-stream");
                    
                    Uri fileUri = createTempFileFromBase64(fileName, base64Data, mimeType);
                    if (fileUri != null) {
                        attachmentUris.add(fileUri);
                    }
                }
            }            // Choose intent action based on whether we have attachments
            if (attachmentUris.size() > 0) {
                emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.setType("message/rfc822");
                emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachmentUris);
                emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:"));
            }
            
            emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, message);

            this.cordova.getActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."));
            callbackContext.success("Email sent successfully");
        } catch (JSONException e) {
            callbackContext.error("JSON parsing error: " + e.getMessage());
        } catch (android.content.ActivityNotFoundException ex) {
            callbackContext.error("There are no email clients installed.");
        } catch (Exception e) {
            callbackContext.error("Error sending email: " + e.getMessage());
        }
    }
    
    private Uri createTempFileFromBase64(String fileName, String base64Data, String mimeType) {
        try {
            // Remove data URL prefix if present (e.g., "data:image/png;base64,")
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }

            // Decode base64 data
            byte[] decodedData = Base64.decode(base64Data, Base64.DEFAULT);

            // Create temporary file
            File tempDir = new File(this.cordova.getActivity().getCacheDir(), "email_attachments");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            File tempFile = new File(tempDir, fileName);
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(decodedData);
            fos.close();

            // Return file URI using FileProvider for Android 7.0+
            return FileProvider.getUriForFile(
                this.cordova.getActivity(),
                this.cordova.getActivity().getPackageName() + ".fileprovider",
                tempFile
            );

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }
}