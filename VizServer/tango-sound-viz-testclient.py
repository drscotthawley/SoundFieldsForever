#! /usr/bin/env python2.7
#Socket client example in python
from __future__ import print_function

import socket   #for sockets
import sys  #for exit
import numpy as np
import struct


#host = '10.0.1.3';
host = '127.0.0.1';   # IP address or host name
port = 5001;
bufsize = 1000000



# from https://stackoverflow.com/questions/17667903/python-socket-receive-large-amount-of-data
def send_msg(sock, msg):
    # Prefix each message with a 4-byte length (network byte order)
    msg = struct.pack('>I', len(msg)) + msg
    sock.sendall(msg)
    return

def recv_msg(sock):
    # Read message length and unpack it into an integer
    raw_msglen = recvall(sock, 4)
    if not raw_msglen:
        return None
    msglen = struct.unpack('>I', raw_msglen)[0]
    # Read the message data
    return recvall(sock, msglen)

def recvall(sock, n):
    # Helper function to recv n bytes or return None if EOF is hit
    data = ''
    while len(data) < n:
        packet = sock.recv(n - len(data))
        if not packet:
            return None
        data += packet
    return data




def data_to_tuples(x,y,z):
    return zip(x,y,z)

def tuples_to_string(tuples):
    n = len(tuples)
    #print("tuples = ",tuples)
    mystr = ''
    for i in range(n):
        m = len(tuples[i])
        for j in range(m):
            #print("tuples[i][j] = ",tuples[i][j])
            mystr += str(tuples[i][j])
            if (j < m-1):
                mystr += ', '
        mystr += '\n'
    return mystr


def read_file(filename='proc_data10.txt'):
    print("Reading from file",filename)
    with open(filename, 'r') as file:
        rows = [[float(q) for q in (line.strip()).split(',') ] for line in file]
        print("line = ",line)
        cols = [list(col) for col in zip(*rows)]
        #print("cols = ",cols)
        x = cols[0]
        y = cols[1]
        z = cols[2]
        db = cols[3]
        #x = np.float(x)
        #x = np.array(x) * np.random.rand()
    return x, y, z, db


#-----------create an INET, STREAMing socket
try:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
except socket.error:
    print('Failed to create socket')
    sys.exit()
     
print('Socket Created')
 

try:
    remote_ip = socket.gethostbyname( host )
except socket.gaierror:
    #could not resolve
    print('Hostname could not be resolved. Exiting')
    sys.exit()

connected = False
while not connected:
    try:
        s.connect((remote_ip , port))         #Connect to remote server
        connected = True
    except:
        pass

 
print('Socket Connected to ' + host + ' on ip ' + remote_ip)


#------------ Now that we're connected, send data

# setup the data
#x = np.random.rand(100)
#y = np.random.rand(100)
#db = np.sin(x)+np.cos(y)

x,y,z,db = read_file()
 
x_range = np.max(x) - np.min(x)
y_range = np.max(y) - np.min(y)
z_range = np.max(z) - np.min(z)
print(" x_range, y_range, z_range = ",x_range, y_range, z_range)



#Send some data to remote server
#message = "GET / HTTP/1.1\r\n\r\n"
tuples = data_to_tuples(x,y,db)
message = tuples_to_string(tuples)


print("Sending message.")#"  Message = ",message)
 
try :
    #Set the whole string
    #s.sendall(message)
    send_msg(s, message)
except socket.error:
    #Send failed
    print('Send failed')
    sys.exit()

print('Message sent.')

 
#Now receive data
reply = s.recv(bufsize)
#reply = recv_msg(s)

print("reply = ",reply)
reply_str = reply # reply.decode('utf-8') 



