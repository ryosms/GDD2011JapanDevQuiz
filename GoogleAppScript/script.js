function gddDevQuiz() {
  var spreadSheet = SpreadsheetApp.getActiveSpreadsheet();
  
  // いらないシート削除
  var sheets = spreadSheet.getSheets();
  var cnt = sheets.length - 1;
  for(var i = cnt; i > 0; i--) {
    spreadSheet.setActiveSheet(sheets[i]);
    spreadSheet.deleteActiveSheet();
    spreadSheet.setActiveSheet(sheets[i - 1]);
  }

  
  // json取得
  var url = "http://gdd-2011-quiz-japan.appspot.com/apps_script/data?param=-2984875658343341476";
  //var url = "http://gdd-2011-quiz-japan.appspot.com/apps_script/sample";
  var response = UrlFetchApp.fetch(url);
  if(response.getResponseCode() != 200) {
    return 0;
  }
  var jsonString = response.getContentText();
  var jsonObj = eval("(" + jsonString + ")");
  var jsonCnt = jsonObj.length;
  if(jsonCnt == 0) {
    return 0;
  }
  var cityName = jsonObj[i].city_name;
  sheets[0].setName(cityName);
  sheets = spreadSheet.getSheets();
  var citySheet = spreadSheet.setActiveSheet(sheets[i]);
  var subJson = eval(jsonObj[0].data);
  var dataCnt = subJson.length;
  for(var j = 0; j < dataCnt; j++) {
    var row = j + 1;
    var capacity = subJson[j].capacity;
    var usage = subJson[j].usage;
    citySheet.getRange("A" + row).setValue(capacity);
    citySheet.getRange("B" + row).setValue(usage);
    citySheet.getRange("C" + row).setValue(((usage * 100) / capacity) + "%");
  }
  
  for(var i = 1; i < jsonCnt; i++) {
    cityName = jsonObj[i].city_name;
    spreadSheet.insertSheet(cityName);
    sheets = spreadSheet.getSheets();
    citySheet = spreadSheet.setActiveSheet(sheets[i]);
    subJson = eval(jsonObj[i].data);
    dataCnt = subJson.length;
    for(var j = 0; j < dataCnt; j++) {
      row = j + 1;
      capacity = subJson[j].capacity;
      usage = subJson[j].usage;
      citySheet.getRange("A" + row).setValue(capacity);
      citySheet.getRange("B" + row).setValue(usage);
      citySheet.getRange("C" + row).setValue(((usage * 100) / capacity) + "%");
    }
  }
}
