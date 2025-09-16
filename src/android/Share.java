package mh.plugins.share_files_to_email;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Share extends CordovaPlugin {

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
            }
            
            // Choose intent action based on whether we have attachments
            if (attachmentUris.size() > 0) {
                emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                emailIntent.setType("message/rfc822");
                emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachmentUris);
                emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                emailIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } else {
                emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:"));
            }

            emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, message);

            // Check if intent can be resolved before starting
            if (emailIntent.resolveActivity(this.cordova.getActivity().getPackageManager()) != null) {
                if (attachmentUris.size() > 0) {
                    this.cordova.getActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."));
                } else {
                    this.cordova.getActivity().startActivity(emailIntent);
                }
                callbackContext.success("Email sent successfully");
            } else {
                callbackContext.error("No email apps available to handle this request");
                return;
            }
            
        } catch (JSONException e) {
            callbackContext.error("JSON parsing error: " + e.getMessage());
        } catch (android.content.ActivityNotFoundException ex) {
            callbackContext.error("There are no email clients installed.");
        } catch (Exception e) {
            callbackContext.error("Error sending email: " + e.getMessage());
        }
    }

    private Uri createTempFileFromBase64(String fileName, String base64Data, String mimeType) {
        String sanitizedFileName = sanitizeFileName(fileName);
        
        try {
            // Remove data URL prefix if present
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }

            // Decode base64 data
            if (base64Data == null || base64Data.trim().isEmpty()) {
                return null;
            }
            
            String trimmedData = base64Data.trim().replaceAll("\\s", "");
            while (trimmedData.length() % 4 != 0) {
                trimmedData += "=";
            }
            
            byte[] decodedData = Base64.decode(trimmedData, Base64.DEFAULT);
            if (decodedData.length == 0) {
                return null;
            }

            // Create temporary file
            File tempDir = new File(this.cordova.getActivity().getCacheDir(), "email_attachments");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            File tempFile = new File(tempDir, sanitizedFileName);
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(decodedData);
            fos.close();
            
            if (!tempFile.exists() || tempFile.length() == 0) {
                return null;
            }

            // Use existing Cordova FileProvider
            String authority = this.cordova.getActivity().getPackageName() + ".cdv.core.file.provider";
            Uri fileUri = FileProvider.getUriForFile(this.cordova.getActivity(), authority, tempFile);
            return fileUri;

        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }
    
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "attachment";
        }
        
        String sanitized = fileName.replaceAll("[<>:\"|?*\\\\/]", "_");
        sanitized = sanitized.trim().replaceAll("^\\.*", "").replaceAll("\\.*$", "");
        
        if (sanitized.isEmpty()) {
            sanitized = "attachment";
        }
        
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