#! /usr/bin/env python2.7
from __future__ import print_function
import socket
import sys
import numpy as np
import matplotlib
matplotlib.use('Qt4Agg')
import matplotlib.pyplot as plt
import csv
import struct

already_plotted = False


HOST = ''   # IP address or hostname
PORT = 5001 # Arbitrary non-privileged port
bufsize = 4096

fig, ax = plt.subplots(1,1, sharex=True, sharey=True)


# from https://stackoverflow.com/questions/17667903/python-socket-receive-large-amount-of-data
def send_msg(sock, msg):
    # Prefix each message with a 4-byte length (network byte order)
    msg = struct.pack('>I', len(msg)) + msg
    sock.sendall(msg)
    return
 
def recvall(sock, n):
    # Helper function to recv n bytes or return None if EOF is hit
    data = ''
    while len(data) < n:
        packet = sock.recv(n - len(data))
        if not packet:
            return None
        data += packet
    return data

def recv_msg(sock):
    # Read message length and unpack it into an integer
    raw_msglen = recvall(sock, 4)
    if not raw_msglen:
        return None
    msglen = struct.unpack('>I', raw_msglen)[0]
    # Read the message data
    return recvall(sock, msglen)



def make_plot(x,y,z):
    global already_plotted
    if (already_plotted):
        ax.clear()
    #ax.tripcolor(x,y,z)
    ax.tricontourf(x,y,z, 20) # choose 20 contour levels, just to show how good its interpolation is
    plt.plot(x,y,'ko')   # plot the locations of the sensor

    x_range = np.max(x) - np.min(x)
    y_range = np.max(y) - np.min(y)
    maxrange = max(x_range, y_range)
    print("maxrange = ",maxrange)

    #plt.xlim(-.5, .5)
    #plt.ylim(-.9, 0.1)
    plt.gca().set_aspect('equal', adjustable='box-forced')
 
    plt.ion() 
    plt.draw()
    plt.show()
    plt.pause(0.1)
    if (not already_plotted):
    #    plt.show()
        already_plotted = True
    return

def str_to_list(instr):
    lines = instr.splitlines()
    reader = csv.reader(lines, delimiter=',')
    data = []
    for row in reader:
        data.append(row)
    return data
 
def plot_using_string(bigstr):
    #print("plotting, bigstr = {",bigstr,"}")
    if ("" == bigstr):
        return
    biglist = str_to_list(bigstr)
    bigarray = np.array(biglist,dtype=np.float32)
    #print("bigarray.shape = ",bigarray.shape)
    #print("bigarray = ",bigarray)
    npoints = int(bigarray.shape[0])
    if (npoints >=3):
        bigarray = np.resize(bigarray,[npoints,3])
        x = bigarray[:,0]
        y = bigarray[:,1]
        z = bigarray[:,2]
        make_plot(x,y,z)
    return


s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
print('Server Socket created')

try:
    s.bind((HOST, PORT))
except socket.error as serror:
    msg = serror.strerror
    print('Bind failed. Error Code: ' + str(msg))
    sys.exit()
     
print('Socket bind complete')
 
s.listen(10)
print('Socket now listening on port '+str(PORT))
 
#conn, addr = s.accept()

bigstr = ""
while 1:
    plt.pause(3)
    data = ''

    print("in socket waiting loop")

    conn, addr = s.accept()  # this is blocking, i.e. execution pauses until connection occurs
    print('Connected with ' + addr[0] + ':' + str(addr[1]))
    
    #data = conn.recv(bufsize)
    data = recv_msg(conn)

    recv_str = data.decode('utf-8')  
    print("Received string: ",recv_str)
    #print("len(recv_str) = ",len(recv_str))

    reply ="Data received."             # tell the client we got the data
    conn.sendall(reply.encode())
 
    if ("" != recv_str) and ("\n" != recv_str):
        bigstr += recv_str      # append received string onto big string
        print("Plotting...")
        plot_using_string(bigstr)
    
 
conn.close()
s.close()