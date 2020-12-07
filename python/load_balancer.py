from flask import Flask, request, url_for

app = Flask(__name__)
counter = 0
serverCounts = {}

@app.route("/getPortNum/<campus>")
def getPortNumber(campus):
    global counter
    campusLiteral = request.view_args['campus']
    portNum = ""
    if campus == "CON":
        portNum = "1234"
    elif campus == "MON":
        portNum = "1237"
    elif campus == "MCG":
        if len(serverCounts) == 0:
            portNum = "1235"
            serverCounts[portNum] = 1
            counter += 1
        else:
            if counter%2 != 0:
                portNum = "1236"
                if portNum not in serverCounts:
                    serverCounts[portNum] = 0
                serverCounts[portNum] = serverCounts[portNum]+1
                counter += 1
            else:
                portNum = "1235"
                serverCounts[portNum] = serverCounts[portNum]+1
                counter += 1
    return { "portNum": portNum }

if __name__ == "__main__":
    app.run(debug=True)
