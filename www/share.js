module.exports = function(text, title, emailArray, files, success, error){
  if(typeof text !== "string") {
    text = "";
  }
  if(typeof title !== "string") {
    title = "Share";
  }
  if(!Array.isArray(emailArray)) {
    emailArray = [];
  }
  if(!Array.isArray(files)) {
    files = [];
  }
  cordova.exec(success, error, "Share", "share", [text, title, emailArray, files]);
  return true;
};