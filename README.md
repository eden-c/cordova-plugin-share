# Cordova Email Plugin with Attachments

A Cordova plugin for sending emails with attachment support on Android and iOS. This plugin allows you to compose emails with multiple recipients and base64 file attachments.

## Features

- Send emails with custom subject and body
- Multiple recipient support
- Base64 file attachments (images, documents, etc.)
- Direct email app integration

## Installation

### Install from Local Path
```bash
ionic cordova plugin add /path/to/cordova-plugin-share
```

### Install from Git Repository
```bash
ionic cordova plugin add https://github.com/eden-c/cordova-plugin-share.git
```

## Usage

### Basic Email (No Attachments)
```javascript
navigator.share(
  "Email body content",           // message
  "Email Subject",                // subject  
  ["recipient@example.com"],      // email recipients array
  [],                            // files array (empty for no attachments)
  function success() {
    console.log("Email sent successfully");
  },
  function error(err) {
    console.error("Email failed:", err);
  }
);
```

### Email with Attachments
```javascript
const files = [
  {
    fileName: "document.pdf",
    base64: "JVBERi0xLjQK...", // your base64 string (without data URL prefix)
    mimeType: "application/pdf"
  },
  {
    fileName: "image.png",
    base64: "iVBORw0KGgoA...", // your base64 string
    mimeType: "image/png"
  }
];

navigator.share(
  "Please find attached files.",
  "Files Attached",
  ["recipient1@example.com", "recipient2@example.com"],
  files,
  function success() {
    console.log("Email with attachments sent successfully");
  },
  function error(err) {
    console.error("Email failed:", err);
  }
);
```

### TypeScript Usage
```typescript
// Type assertion for TypeScript projects
(navigator as any).share(
  message,
  subject,
  recipients,
  files,
  successCallback,
  errorCallback
);
```

## API Reference

### navigator.share(message, subject, recipients, files, success, error)

#### Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `message` | String | Email body content |
| `subject` | String | Email subject line |
| `recipients` | Array | Array of email addresses |
| `files` | Array | Array of file objects for attachments |
| `success` | Function | Success callback function |
| `error` | Function | Error callback function |

#### File Object Structure

```javascript
{
  fileName: "example.pdf",        // Required: File name with extension
  base64: "base64DataString",     // Required: Base64 encoded file data
  mimeType: "application/pdf"     // Optional: MIME type (defaults to application/octet-stream)
}
```

## Supported File Types

The plugin supports any file type that can be base64 encoded:

- **Images**: PNG, JPG, GIF, etc.
- **Documents**: PDF, DOC, TXT, etc.
- **Archives**: ZIP, RAR, etc.
- **Other**: Any binary or text file

## Platform Support

- **Android**: API level 16+ (Android 4.1+)
- **iOS**: iOS 9.0+

## Important Notes

### Base64 Data
- Remove data URL prefixes (e.g., `data:image/png;base64,`) before passing to the plugin
- Ensure base64 strings are complete and valid
- Large files may cause memory issues on older devices

### Android Permissions
The plugin automatically handles file sharing permissions using FileProvider for Android 7.0+.

### File Size Limitations
- Email providers typically limit attachment sizes (usually 25MB total)
- Consider file size when attaching multiple files
- Base64 encoding increases file size by ~33%

## Error Handling

Common error scenarios:
- No email clients installed
- Invalid base64 data
- File size too large
- Network connectivity issues


## License

Apache 2.0 License

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## Changelog

### v0.1.1
- Added base64 file attachment support
- Updated to use modern email intents
- Improved email app targeting
- Added FileProvider support for Android 7.0+

