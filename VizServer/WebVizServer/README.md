

# Visualizing Real-Time Point-Cloud Sound Intensity Data via Voronoi Tesselation

This is for the Google Tango app project, which uses a client-server model whereby the Tango ("client")
sends its measurements via WebSockets to this vizualization server.

Here's a prototype of our sound viz server.

You can pan and zoom. Yay!

This works best with the Chrome browser; Firefox is slow.


## Setup/Installation


* Set up Node.js and NPM.  Instructions: [How to Install Node.js and NPM on a Mac](http://blog.teamtreehouse.com/install-node-js-npm-mac)
* Then from Terminal within the WebVizServer diretory, run `npm install` 

...and that should be it!


## To Run:

Start the server from the Terminal

	node index.js &
	
Then point your web browser to this url: <http://localhost:3000>


You can test the socket capability by running the Python (3) client in Terminal:

	python3 test_client.py

...and see what it does to the screen on your web browser!
<hr>
Copyright (2017) Scott Hawley

