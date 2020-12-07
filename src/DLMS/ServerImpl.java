package DLMS;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.SocketException;
import java.rmi.server.RemoteRef;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public class ServerImpl extends UnicastRemoteObject implements ServerInterface {

    class Admin{
        String adminID = " ";
    }
    class User{
        String userID = " ";
        int borrowCount = 0;
    }
    class Item{
        String ID;
        String name;
        int num;
    }

    HashMap<String, Item> items = new HashMap<>();
    HashMap<String, ArrayList<String> > waitList = new HashMap<>();
    HashMap<String, ArrayList<String> > borrowedItems = new HashMap<>();
    private String Campus = " ";

    ArrayList<Admin> adminClients = new ArrayList<>();
    ArrayList<User> userClients = new ArrayList<>();

    public ServerImpl() throws java.rmi.RemoteException{
        super();
    }

    public static void Log(String serverID,String Message) throws Exception{

        String path = "D:\\Sem6\\DS\\DLMS\\logs\\" + serverID + "_Server.log";
        FileWriter fileWriter = new FileWriter(path,true);
        BufferedWriter bf = new BufferedWriter(fileWriter);
        bf.write(Message + "\n");
        bf.close();
    }

    @Override
    public RemoteRef getRef() {
        return super.getRef();
    }

    public String getFormatDate(){
        Date date = new Date();
        long times = date.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(date);
        return dateString;
    }

    public void StartServer(String campus) {
        Campus = campus;
        try {
            Log(Campus,getFormatDate() + " Server for " + Campus + " started");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Item i1 = new Item();
        i1.name = "Kafka on the Shore";
        i1.num = 2;
        i1.ID = Campus+"1111";
        Item i2 = new Item();
        i2.name = "Norwegian Wood";
        i2.num = 1;
        i2.ID = Campus+"2222";
        Item i3 = new Item();
        i3.name = "Dance Dance Dance";
        i3.num = 5;
        i3.ID = Campus+"3333";
        items.put(i1.ID,i1);
        items.put(i2.ID,i2);
    }

    public boolean managerLogin(String managerID) {

        Boolean exist = false;

        for(int i = 0; i < adminClients.size(); i ++){
            if(adminClients.get(i).adminID.equals(managerID)){
                exist = true;
                break;
            }
        }

        if(!exist){
            Admin newAdmin = new Admin();
            newAdmin.adminID = managerID;
            adminClients.add(newAdmin);
        }
        System.out.println("ManagerClient [" + managerID + "] log in successfully");
        try {
            Log(Campus, getFormatDate() + " ManagerClient [" + managerID + "] log in successfully" );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean userLogin(String studentID) {

        Boolean exist = false;

        for(int i = 0; i < userClients.size(); i ++){
            if(userClients.get(i).userID.equals(studentID)){
                exist = true;
                break;
            }
        }
        if(!exist){
            User newStudent = new User();
            newStudent.userID = studentID;
            newStudent.borrowCount = 0;
            userClients.add(newStudent);
        }
        System.out.println("UserClient " + studentID + " log in successfully");
        try {
            Log(Campus, getFormatDate() + " UserClient " + studentID + " log in successfully" );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public String addItem (String managerID, String itemID, String itemName, int quantity) {
        String result = "";
        String error = "";
        if(itemID.isEmpty() || itemName.isEmpty()){
            return result;
        }

        synchronized(this) {
            if(quantity >= 0){
                if (items.containsKey(itemID)){
                    if(itemName.equals(items.get(itemID).name)) {
                        items.get(itemID).num += quantity;
                        result = "Manager increase quantity of item [" + itemID + "] by [" + quantity + "] success";
                    }else{
                        error = "Manager increase quantity of item [" + itemID + "] by [" + quantity + "] failed : " +
                                "Wrong ItemName";
                    }
                }else {
                    int flag = 0;
                    for (HashMap.Entry<String, Item> entry : items.entrySet()) {
                        if (entry.getValue().name.equals(itemName)) {
                            error = "Manager add [" + quantity + "] of item [" + itemID + "] failed : " +
                                    "ItemName already exist";
                            flag = 1;
                        }
                    }
                    if (flag == 0) {
                        Item newItem = new Item();
                        newItem.name = itemName;
                        newItem.num = quantity;
                        items.put(itemID, newItem);
                        result = "Manager add [" + quantity + "] of item [" + itemID + "] success";
                    }
                }
            }
            else{
                error = "Manager add [" + quantity + "] of item [" + itemID + "] failed : Qauntity must more than 0";
            }
        }

        if(!result.isEmpty()){
            System.out.println(result);
            try {
                Log(Campus, getFormatDate() + result);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(waitList.containsKey(itemID) && waitList.get(itemID).size() > 0) {
                String lendResult = autoLend(itemID);
                if (!lendResult.isEmpty()) {
                    String log = " Server auto lend item ["+itemID+"] " +
                            "to user : " + lendResult+" after manager ["+managerID+"] add item. ";
                    try {
                        System.out.println(log);
                        Log(Campus, getFormatDate() + log);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    String log1 = " Server auto lend item ["+itemID+"] " +
                            "to user failed after manager ["+managerID+"] add item. ";
                    try {
                        System.out.println(log1);
                        Log(Campus, getFormatDate() + log1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }else{
            try {
                Log(Campus, getFormatDate() + error);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public String listItemAvailability (String managerID) {
        String result = "";
        for(HashMap.Entry<String, Item> entry : items.entrySet()){
            result = result + entry.getKey() + " " + entry.getValue().name + " " + entry.getValue().num + " , ";
        }
        if(result.isEmpty()){
            String log = " Manager " + managerID + " list all of item failed";
            System.out.println(log);
            try {
                Log(Campus, getFormatDate() + log);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            String log1 = " Manager " + managerID + " list all of item success. "
                    + "All Items: " + result;
            System.out.println(log1);
            try {
                Log(Campus, getFormatDate() + log1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public String removeItem (String managerID, String itemID, int quantity) {
        String result = "";
        String error = "";
        synchronized(this) {
            if(items.containsKey(itemID)){
                if(items.get(itemID).num > 0){
                    if(quantity < 0 ){
                        //remove all
                        items.remove(itemID);
                        result = " Manager [" + managerID + "] delete item [" + itemID + "] success. ";
                        if(waitList.containsKey(itemID)){
                            waitList.remove(itemID);
                            result += "Delete this item from wait list. ";
                        }if(borrowedItems.containsKey(itemID)){
                            borrowedItems.remove(itemID);
                            result += "Delete this item from borrowed list ";
                        }
                    }else if(quantity <= items.get(itemID).num) {
                        items.get(itemID).num -= quantity;
                        result = " Manager [" + managerID + "] remove ["
                                + quantity + "] of item [" + itemID + "] success.";
                    }else{
                        error = "No Enough Item To Remove";
                    }
                }else{
                    error = "No Item Available At This Moment";
                }
            }else{
                error = "Item Not Found! ";
            }
        }
        if(!result.isEmpty()) {
            try {
                Log(Campus, getFormatDate() + result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            String log = " Manager [" + managerID + "] remove ["
                    + quantity + "] of item [" + itemID + "] failed: ";
            log += error;
            try {
                Log(Campus, getFormatDate() +log);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public String borrowItem (String campusName, String userID, String itemID, int numberOfDays) {
        String result = "";
        String command = "borrowItem(" + userID + "," + itemID + "," + numberOfDays + ")";

        for(int i = 0; i < userClients.size();i ++) {
            if (userClients.get(i).userID.equals(userID)) {
                try {
                    if (campusName.equals(Campus)) {
                        result = borrowLocal(userID, itemID);
                    } else if (campusName.equals("CON")) {
                        int serverport = 2234;
                        result = UDPRequest.UDPborrowItem(command, serverport);
                    } else if (campusName.equals("MCG")) {
                        int serverport = 2235;
                        result = UDPRequest.UDPborrowItem(command, serverport);
                    } else if (campusName.equals("MON")) {
                        int serverport = 2236;
                        result = UDPRequest.UDPborrowItem(command, serverport);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if(!campusName.equals(Campus)) {
                    if (result.isEmpty()) {
                        String log = " Server borrow item ["+itemID+"] for user ["+userID+"] from server ["+campusName+"] failed";
                        System.out.println(log);
                        try {
                            Log(Campus, getFormatDate() + log);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        String log2 = " Server borrow item ["+itemID+"] for user ["+userID+"] from server ["+campusName+"] success";
                        System.out.println(log2);
                        try {
                            Log(Campus, getFormatDate() + log2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }
        return result;
    }

    @Override
    public String waitInQueue(String campusName, String userID, String itemID) {
        String result = " ";
        String command = "waitInQueue(" + userID + "," + itemID + ")";
        for (User temp : userClients) {
            if (temp.userID.equals(userID)) {
                try {
                    if (campusName.equals(Campus)) {
                        result = waitInLocal(userID, itemID);
                    } else if (campusName.equals("CON")) {
                        int serverport = 2234;
                        result = UDPRequest.UDPwaitInQueue(command, serverport);
                    } else if (campusName.equals("MCG")) {
                        int serverport = 2235;
                        result = UDPRequest.UDPwaitInQueue(command, serverport);
                    } else if (campusName.equals("MON")) {
                        int serverport = 2236;
                        result = UDPRequest.UDPwaitInQueue(command, serverport);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    @Override
    public String findItem (String userID, String itemName) {
        String result = "";
        result = findItemLocal(itemName);
        System.out.println("IN ServerImpl.findItem(): " + result);
        String command = "findItem(" + itemName + ")";

//        try {
//            if(Campus.equals("CON")){
//                int serverport1 = 2235;
//                int serverport2 = 2236;
//                result = result + " " + UDPRequest.UDPfindItem(command, serverport1);
//                result = result + " " + UDPRequest.UDPfindItem(command, serverport2);
//            }
//            else if(Campus.equals("MCG")){
//                int serverport1 = 2234;
//                int serverport2 = 2236;
//                result = result + " " + UDPRequest.UDPfindItem(command, serverport1);
//                result = result + " " + UDPRequest.UDPfindItem(command, serverport2);
//            }
//            else{
//                int serverport1 = 2234;
//                int serverport2 = 2235;
//                result = result + " " + UDPRequest.UDPfindItem(command, serverport1);
//                result = result + " " + UDPRequest.UDPfindItem(command, serverport2);
//            }
//        } catch (SocketException e1) {
//            e1.printStackTrace();
//        }
        if(!result.isEmpty()) {
            String log =" User [" + userID + "] found all item named ["+itemName +"] success . Items: "+result;
            System.out.println(log);
            try {
                Log(Campus, getFormatDate() + log );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            String log1 =" User [" + userID + "] found all item named ["+itemName +"] failed. ";
            System.out.println(log1);
            try {
                Log(Campus, getFormatDate() + log1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public String returnItem(String campusName, String userID, String itemID) {
        String result = "";
        String command = "returnItem(" + itemID + "," + userID + ")";
        int serverPort;

        try {
            if(campusName.equals(Campus)){
                result = returnLocal(itemID,userID);

            }
            else if(campusName.equals("CON")){
                serverPort = 2234;
                result = UDPRequest.UDPreturnItem(command,serverPort);

            }
            else if(campusName.equals("MCG")){
                serverPort = 2235;
                result = UDPRequest.UDPreturnItem(command,serverPort);

            }
            else if(campusName.equals("MON")){
                serverPort = 2236;
                result = UDPRequest.UDPreturnItem(command,serverPort);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if(!campusName.equals(Campus)) {
            if (!result.isEmpty()) {
                String log = " Server return item [" + itemID + "] for user [" + userID + "] to server ["
                        + campusName + "] success";
                System.out.println(log);
                try {
                    Log(Campus, getFormatDate() + log);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                String log1 = " Server return item [" + itemID + "] for user [" + userID + "] to server ["
                        + campusName + "] failed";
                System.out.println(log1);
                try {
                    Log(Campus, getFormatDate() + log1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    @Override
    public String listBorrowedItem(String userID) {
        String result = "";
        result = listBorrowedLocal(userID);
        System.out.println("IN ServerImpl.listBorrowedItem(): " + result);
        String command = "listBorrowedItem(" + userID + ")";

//        try {
//            if(Campus.equals("CON")){
//                int serverport1 = 2235;
//                int serverport2 = 2236;
//                result += UDPRequest.UDPlistBorrowedItem(command, serverport1);
//                result += UDPRequest.UDPlistBorrowedItem(command, serverport2);
//            }
//            else if(Campus.equals("MCG")){
//                int serverport1 = 2234;
//                int serverport2 = 2236;
//                result += UDPRequest.UDPlistBorrowedItem(command, serverport1);
//                result += UDPRequest.UDPlistBorrowedItem(command, serverport2);
//            }
//            else if(Campus.equals("MON")){
//                int serverport1 = 2234;
//                int serverport2 = 2235;
//                result += UDPRequest.UDPlistBorrowedItem(command, serverport1);
//                result += UDPRequest.UDPlistBorrowedItem(command, serverport2);
//            }
//        } catch (SocketException e1) {
//            e1.printStackTrace();
//        }

        return result;
    }

    public String listBorrowedLocal(String userID){
        String result = "";
        for(HashMap.Entry<String,ArrayList<String>> entry : borrowedItems.entrySet()){
            if(entry.getValue().contains(userID)){
                result += entry.getKey()+" "+items.get(entry.getKey()).name+", ";
            }
        }
        return result;
    }

    public String returnLocal(String itemID,String userID) {
        String result = "";
        synchronized (this) {
            if (borrowedItems.containsKey(itemID)) {
                if (borrowedItems.get(itemID).contains(userID)) {
                    borrowedItems.get(itemID).remove(userID);
                    items.get(itemID).num++;
                    result = itemID;
                    String log = " User [" + userID + "] return item [" + itemID + "] success.";
                    System.out.println(log);
                    try {
                        Log(Campus, getFormatDate() + log);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if(waitList.containsKey(itemID) && waitList.get(itemID).size() > 0) {
                        String lendResult = autoLend(itemID);
                        if (!lendResult.isEmpty()) {
                            String log2 = " Server auto lend item ["+itemID+"] " +
                                    "to user : " + lendResult+" after user ["+userID+"] return.";
                            System.out.println(log2);
                            try {
                                Log(Campus, getFormatDate() + log2);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            result = log2;
                        } else {
                            String log3 = " Server auto lend item ["
                                    +itemID+"] failed after user ["+userID+"] return.";
                            try {
                                System.out.println(log3);
                                Log(Campus, getFormatDate() + log3);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            result = log3;
                        }
                    }

                }
            }
        }
        if (result.isEmpty()){
            String log1 = " User [" + userID + "] return item [" + itemID + "] failed.";
            System.out.println(log1);
            try {
                Log(Campus, getFormatDate() + log1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public String findItemLocal(String itemName){
        String result = "";
        int availableNum = 0;
        String itemID = "";
        synchronized(this) {
            for(HashMap.Entry<String,Item> entry : items.entrySet()){
                if(entry.getValue().name.equals(itemName)){
                    availableNum += entry.getValue().num;
                    itemID = entry.getKey();
                    result = itemID + " " + Integer.toString(availableNum);
                }
            }
        }
        return result;
    }

    public String waitInLocal(String userID, String itemID){
        String result = " ";
        synchronized (this) {

            if(!waitList.containsKey(itemID)){
                ArrayList<String> users = new ArrayList<>();
                users.add(userID);
                waitList.put(itemID,users);
                result = String.valueOf(waitList.get(itemID).indexOf(userID)+1);
            }else{
                if(!waitList.get(itemID).contains(userID)){
                    waitList.get(itemID).add(userID);
                    result = String.valueOf(waitList.get(itemID).indexOf(userID)+1);
                }
            }

            if (result.equals(" ")) {
                String log = " Server add user [" + userID + "] in wait queue of item ["+itemID+ "] failed.";
                System.out.println(log);
                try {
                    Log(Campus, getFormatDate() + log);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                String log1 = " Server add user [" + userID + "] in wait queue of " +
                        "item ["+itemID+ "] at position [" +result+"] success.";
                System.out.println(log1);
                try {
                    Log(Campus, getFormatDate() + log1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return result;
    }

    public String autoLend (String itemID) {
        String result = "";
        String users = "";
        if (waitList.containsKey(itemID) && waitList.get(itemID).size() > 0 ) {
            int left = items.get(itemID).num;
            int pointer = 0;
            while(left > 0 && waitList.get(itemID).size() > 0 && pointer < waitList.get(itemID).size() ) {
                String waitUser = waitList.get(itemID).get(pointer);
                result = borrowLocal(waitUser, itemID);
                if(!result.isEmpty()){
                    waitList.get(itemID).remove(waitUser);
                    users += waitUser+",";
                }else{
                    pointer ++;
                }
                left = items.get(itemID).num;
            }
        }
        return users;
    }

    public String borrowLocal(String userID, String itemID){
        String result = "";
        String failReason = "";
        int flag = 0;
        String userCampus = userID.substring(0,3);
        synchronized (this){
            if(!userCampus.equals(Campus)){
                for(HashMap.Entry<String, ArrayList<String>> entry : borrowedItems.entrySet()){
                    if(entry.getValue().contains(userID)){
                        flag = 1;
                        failReason = "User can only borrow 1 item from other libraries";
                    }
                }
            }
            if(flag == 0) {
                if (items.get(itemID).num > 0) {
                    if (!borrowedItems.containsKey(itemID)) {
                        ArrayList<String> newBorrowedUser = new ArrayList<>();
                        newBorrowedUser.add(userID);
                        borrowedItems.put(itemID, newBorrowedUser);
                        items.get(itemID).num--;
                    } else {
                        if (!borrowedItems.get(itemID).contains(userID)) {
                            borrowedItems.get(itemID).add(userID);
                            items.get(itemID).num--;
                        }
                    }
                    result = itemID;
                }else{
                    failReason = "No item left";
                }
            }
            if (result.isEmpty()) {
                String log = " User [" + userID + "] borrow item ["+itemID+"] failed: ";
                System.out.println(log);
                try {
                    Log(Campus, getFormatDate() + log +failReason);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                String log2 = " User [" + userID + "] borrow item ["+itemID+"] success.";
                System.out.println(log2);
                try {
                    Log(Campus, getFormatDate() + log2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

}
