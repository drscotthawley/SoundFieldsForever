from socketIO_client_nexus import SocketIO, LoggingNamespace

import logging
logging.getLogger('socketIO-client').setLevel(logging.DEBUG)
logging.basicConfig()


def on_bbb_response(*args):
    print('on_bbb_response', args)

with open('sample_data.csv', 'r') as myfile:
    data_string =myfile.read()

hostname = 'hedges.belmont.edu'
port = 3000

with SocketIO(hostname, port, LoggingNamespace) as socketIO:
    #socketIO.emit('chat message', {'xxx': 'yyy'}, on_bbb_response)
    #socketIO.wait_for_callbacks(seconds=1)
    socketIO.emit('chat message',data_string)
    socketIO.wait(seconds=1)