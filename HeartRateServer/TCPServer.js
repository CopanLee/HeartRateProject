var net = require('net');
const SQL = require('./SQLClient');
const readline = require('readline');

var HOST = 'localhost';

var PORT = '7530';

console.log( '======================' );

console.info( 'Server is running on port ' + PORT);

const rl = readline.createInterface({
	input: process.stdin,
	output: process.stdout
});

HiddenPassword('Input SQL password: ', function(password){
	SQL.SQLConnection(password);
});

const server = net.createServer();

server.on( 'connection' , function(socket){
    
    var client = socket.remoteAddress;

	var ID = '';

	var MessageQueue = '';

	console.info( 'New Client connected from ' + client);

	console.log( 'Since ' + getDateTime() );

	const QueueMessage = function(data){
		new Promise(function(){
			MessageQueue += new Buffer(data).toString('ascii');
		})
	}

	const DecodeMessage = function() {
		new Promise(function(){
			if (MessageQueue.indexOf(';') > 0)
			{
				var Message = MessageQueue.substring( MessageQueue.indexOf(':') + 1, MessageQueue.indexOf(';') );
				switch (MessageQueue.substring(0, MessageQueue.indexOf(':'))){
						case 'DeviceID':
						ID = Message;
						console.log('DeviceID: ' + ID);
						console.log( '======================' );
						SQL.insertConnectHistory(ID, getDateTime());
						SQL.insertConnectDevice(ID);
						break;
						case 'DeviceData':
						var Data = MessageQueue.substring( MessageQueue.indexOf(':') + 1, MessageQueue.indexOf(',') );
						var Datetime = MessageQueue.substring( MessageQueue.indexOf(',') + 1, MessageQueue.indexOf(';') );
						SQL.InsertData(ID, Data, Datetime);
						break;
				}
				MessageQueue = MessageQueue.substring( MessageQueue.indexOf(';') + 1, MessageQueue.length);
			}
		})
	}

	try
	{
		socket.on( 'data' , async function(data){
			await QueueMessage(data);
			await DecodeMessage();
		});
	}
	catch (err)
	{
		console.log(err);
	}

	try
	{
		socket.on( 'end' , function(){
			SQL.deleteConnectDevice(ID);
			console.log( ID + ' disconnect. ' + getDateTime());
			console.log( '======================' );
		});
	}
	catch (err)
	{
		console.log(err);
	}


});

server.listen(PORT,HOST)

function getDateTime() {
	
		var date = new Date();
	
		var hour = date.getHours();
		hour = (hour < 10 ? "0" : "") + hour;
	
		var min  = date.getMinutes();
		min = (min < 10 ? "0" : "") + min;
	
		var sec  = date.getSeconds();
		sec = (sec < 10 ? "0" : "") + sec;
	
		var msc  = date.getMilliseconds();

		var year = date.getFullYear();
	
		var month = date.getMonth() + 1;
		month = (month < 10 ? "0" : "") + month;
	
		var day  = date.getDate();
		day = (day < 10 ? "0" : "") + day;
	
		return year + '-' + month + '-' + day + ' ' + hour + ":" + min + ":" + sec + ":" + msc;
	
}

function HiddenPassword(query, callback) {
    var stdin = process.openStdin();
    process.stdin.on("data", function(char) {
        char = char + "";
        switch (char) {
            case "\n":
            case "\r":
            case "\u0004":
                stdin.pause();
                break;
            default:
                process.stdout.write("\033[2K\033[200D" + query + Array(rl.line.length+1).join(""));
                break;
        }
    });

    rl.question(query, function(value) {
        rl.history = rl.history.slice(1);
		callback(value);
		rl.close();
    });
}