var i = 0;
var colorsList = [];
for(;;) {
	var element = document.getElementById('card'+i);
	if(element == null) break;
	var myevent = document.createEvent('MouseEvents');
	myevent.initEvent('click', false, true);
	element.dispatchEvent(myevent);
	colorsList.push(element.style.backgroundColor);
	i++;
}
var j, k;
for(j = 0; j < i; j++) {
	if(colorsList[j] != null) {
		var firstElement = document.getElementById('card' + j);
		var secondElement = null;
		for(k = j + 1; k < i; k++) {
			if(colorsList[k] == colorsList[j]) {
				secondElement = document.getElementById('card' + k);
				break;
			}
		}
		var myevent = document.createEvent('MouseEvents');
		myevent.initEvent('click', false, true);
		firstElement.dispatchEvent(myevent);
		secondElement.dispatchEvent(myevent);
		colorsList[j] = null;
		colorsList[k] = null;
	}
} 
