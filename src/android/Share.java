package mh.plugins.share_files_to_email;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;

import android.util.Log;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class echoes a string called from JavaScript.
 */
public class Share extends CordovaPlugin {

    private static final String TAG = "SharePlugin";
    
    @Override
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
    }

    private void share(String message, String subject, String[] to, JSONArray filesArray,
            CallbackContext callbackContext) {
        try {
            Intent emailIntent;
            ArrayList<Uri> attachmentUris = new ArrayList<>();

            // debugging output
            Log.d(TAG, "Share plugin called with message: " + message + 
                ", subject: " + subject + 
                ", to: " + String.join(", ", to) + 
                ", files: " + filesArray.toString());

            // Process file attachments
            if (filesArray != null && filesArray.length() > 0) {
                for (int i = 0; i < filesArray.length(); i++) {
                    JSONObject fileObj = filesArray.getJSONObject(i);
                    String fileName = fileObj.getString("fileName");
                    String base64Data = fileObj.getString("base64");
                    String mimeType = fileObj.optString("mimeType", "application/octet-stream");

                    // debugging output
                    Log.d(TAG, "Processing file attachment: " + 
                        "fileName: " + fileName + 
                        ", mimeType: " + mimeType + 
                        ", base64Data length: " + base64Data.length());
                    
                    Uri fileUri = createTempFileFromBase64(fileName, base64Data, mimeType);
                    if (fileUri != null) {
                        attachmentUris.add(fileUri);    
                    }
                }
            } // Choose intent action based on whether we have attachments
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
            
            // Send debug info to JavaScript console
            String debugInfo = "Email intent created with " + attachmentUris.size() + " attachments";
            Log.d(TAG, debugInfo);
            
            callbackContext.success("Email sent successfully: " + debugInfo);
        } catch (JSONException e) {
            callbackContext.error("JSON parsing error: " + e.getMessage());
        } catch (android.content.ActivityNotFoundException ex) {
            callbackContext.error("There are no email clients installed.");
        } catch (Exception e) {
            callbackContext.error("Error sending email: " + e.getMessage());
        }
    }

    private Uri createTempFileFromBase64(String fileName, String base64Data, String mimeType) {
        Log.d(TAG, "createTempFileFromBase64 called with fileName: " + fileName + ", mimeType: " + mimeType);
        
        // Sanitize filename - remove/replace invalid characters
        String sanitizedFileName = sanitizeFileName(fileName);
        Log.d(TAG, "Sanitized filename from '" + fileName + "' to '" + sanitizedFileName + "'");
        
        try {
            // Remove data URL prefix if present (e.g., "data:image/png;base64,")
            if (base64Data.contains(",")) {
                String originalLength = String.valueOf(base64Data.length());
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                Log.d(TAG, "Removed data URL prefix, base64 length changed from " + originalLength + " to " + base64Data.length());
            }

            // Decode base64 data
            Log.d(TAG, "Decoding base64 data...");
            byte[] decodedData = Base64.decode(base64Data, Base64.DEFAULT);
            Log.d(TAG, "Successfully decoded base64 data, decoded size: " + decodedData.length + " bytes");

            // Create temporary file
            File tempDir = new File(this.cordova.getActivity().getCacheDir(), "email_attachments");
            if (!tempDir.exists()) {
                Log.d(TAG, "Creating temp directory: " + tempDir.getAbsolutePath());
                tempDir.mkdirs();
            }

            File tempFile = new File(tempDir, sanitizedFileName);
            Log.d(TAG, "Creating temp file: " + tempFile.getAbsolutePath());
            
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(decodedData);
            fos.close();
            Log.d(TAG, "Successfully written " + decodedData.length + " bytes to temp file");

            // Return file URI using FileProvider for Android 7.0+

            String authority = this.cordova.getActivity().getPackageName() + ".fileprovider";
            Log.d(TAG, "Creating FileProvider URI with authority: " + authority);
            
            Uri fileUri = FileProvider.getUriForFile(

              this.cordova.getActivity(),
                authority,
                tempFile
            );
            
            Log.d(TAG, "Successfully created FileProvider URI: " + fileUri.toString());
            return fileUri;

        } catch (IOException e) {
            Log.e(TAG, "IOException in createTempFileFromBase64: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException in createTempFileFromBase64 (likely FileProvider issue): " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception in createTempFileFromBase64: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "attachment";
        }
        
        // Replace invalid characters with underscores
        // Invalid characters for most filesystems: < > : " | ? * \ /
        String sanitized = fileName.replaceAll("[<>:\"|?*\\\\/]", "_");
        
        // Remove leading/trailing spaces and dots
        sanitized = sanitized.trim().replaceAll("^\\.*", "").replaceAll("\\.*$", "");
        
        // Ensure filename is not empty after sanitization
        if (sanitized.isEmpty()) {
            sanitized = "attachment";
        }
        
        // Limit filename length (most filesystems have limits around 255 characters)
        if (sanitized.length() > 200) {
            String extension = "";
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0) {
                extension = sanitized.substring(lastDot);
                sanitized = sanitized.substring(0, lastDot);
            }
            sanitized = sanitized.substring(0, 200 - extension.length()) + extension;
        }
        
        return sanitized;
    }
}