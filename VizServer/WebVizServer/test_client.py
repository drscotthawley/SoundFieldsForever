from socketIO_client_nexus import SocketIO, LoggingNamespace

import logging
logging.getLogger('socketIO-client').setLevel(logging.DEBUG)
logging.basicConfig()


def on_bbb_response(*args):
    print('on_bbb_response', args)


def data_to_tuples(x,y,z):
    return list(zip(x,y,z))

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


def read_file(filename='proc_data10.csv'):
    print("Reading from file",filename)
    with open(filename, 'r') as file:
        #firstline = file.readline()  # throw away the header
        rows = [[float(q) for q in (line.strip()).split(',') ] for line in file]
        cols = [list(col) for col in zip(*rows)]
        #print("cols = ",cols)
        x = cols[0]
        y = cols[1]
        z = cols[2]
        db = cols[3]
        #x = np.float(x)
        #x = np.array(x) * np.random.rand()
    return x, y, z, db

#x,y,z,db = read_file()
#tuples = data_to_tuples(x,y,db)
#data_string = tuples_to_string(tuples)
with open('proc_data10.csv', 'r') as myfile:
    data_string =myfile.read()

with SocketIO('localhost', 3000, LoggingNamespace) as socketIO:
    #socketIO.emit('chat message', {'xxx': 'yyy'}, on_bbb_response)
    #socketIO.wait_for_callbacks(seconds=1)
    socketIO.emit('chat message',data_string)
    socketIO.wait(seconds=1)