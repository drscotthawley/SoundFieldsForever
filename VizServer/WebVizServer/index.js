// Socket.IO Server Handling

var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);


app.get('/', function(req, res){
res.sendFile(__dirname + '/index.html');
});

io.on('connection', function(socket){
  //console.log('a user connected');
  //socket.on('disconnect', function(){
  //  console.log('user disconnected');
  //});
  socket.on('chat message', function(msg){   //Not really sure what this does
    io.emit('chat message', msg);
  });
});


http.listen(3000, function(){
	  console.log('listening on *:3000');
});

