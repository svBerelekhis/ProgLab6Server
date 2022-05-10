package server;

import object.ListOfFlat;

public class Printer {
    ServerWorker serverWorker;

    public Printer(ServerWorker serverWorker) {
        this.serverWorker = serverWorker;
    }

    public void print(String str){
        serverWorker.addToAns(str);
        serverWorker.addToAns("\n");
    }
    public void print(int i){

    }
    public void print(ListOfFlat listOfFlat){
        serverWorker.addToAns(listOfFlat);
    }
}
