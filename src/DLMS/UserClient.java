package DLMS;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.rmi.Naming;
import java.util.Scanner;

public class UserClient {

    public static void Log(String ID,String Message) throws Exception {

        String path = "D:\\Sem6\\DS\\DLMS\\logs\\" + ID + "_Manager.log";
        FileWriter fileWriter = new FileWriter(path,true);
        BufferedWriter bf = new BufferedWriter(fileWriter);
        bf.write(Message + "\n");
        bf.close();
    }

    public static String getFormatDate(){
        Date date = new Date();
        long times = date.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(date);
        return dateString;
    }
    public static void main(String[] args) {

        try {
            int RMIPort;
            String hostName;
            String portNum = " ";

            System.out.println("Enter userID:");
            Scanner Id = new Scanner(System.in);
            String userID = Id.nextLine();
            String campus = userID.substring(0,3);

            if(userID.length() != 8) {
                System.out.println("Invalid UserID");
                System.exit(1);
            }
            if(!userID.substring(3,4).equals("U")){
                System.out.println("Invalid UserID");
                System.exit(1);
            }

            URL url = new URL("http://localhost:5000/getPortNum/"+campus);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                if(output.contains(":")) {
                    portNum = output.split(":")[1].trim().replace("\"","");
                    break;
                }
            }
            conn.disconnect();

            System.out.println(getFormatDate() + " " + userID + " attempt to connect campus " + campus + " on port number " + portNum);
            Log(userID, getFormatDate() + " " + userID + " attempt to connect campus " + campus + " on port number " + portNum);

            InputStreamReader is = new InputStreamReader(System.in);
            br = new BufferedReader(is);

            RMIPort = Integer.parseInt(portNum);
            String registryURL = "rmi://localhost:" + portNum + "/DLMS-" + campus;

            ServerInterface h = (ServerInterface)Naming.lookup(registryURL);
            System.out.println("Lookup completed " );

            if(h.userLogin(userID)){
                System.out.println("Log in successfully");
                Log(userID, getFormatDate() + " " + userID + " log in successfully");

                Socket s = new Socket("localhost", 50555);
                DataInputStream dis = new DataInputStream(s.getInputStream());
                SimpleDateFormat time_pattern = new SimpleDateFormat("HH:mm:ss.SSSSSS");
                Date dt = new Date();
                long req_time = dt.getTime();
                long s_time = dis.readLong();
                Date server_time = new Date();
                server_time.setTime(s_time);
                Date actual_time = new Date();
                long res_time = actual_time.getTime();
                System.out.println("|\n|-Time returned by server : " + time_pattern.format(server_time));
                long process_delay_latency = res_time - req_time;
                System.out.println("|\n|-Process Delay Latency : " + (double)process_delay_latency/1000.0 + " seconds");
                System.out.println("|\n|-Actual clock time at client side :" + time_pattern.format(actual_time));
                long client_time = s_time + process_delay_latency/2;
                dt.setTime(client_time);
                System.out.println("|\n|-Synchronzed process client time : " + time_pattern.format(dt));
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
                String sys_time = "" + df.format(dt);
                long error = res_time - client_time;
                System.out.println("|\n|-Synchronization error : " + (double)error/1000.0 + " seconds");
                Log(userID, getFormatDate() + " Synchronization Error: " + (double)error/1000.0 + " seconds");
                dis.close();
                s.close();
            }
            else {
                System.out.println("Log in failed");
                Log(userID, getFormatDate() + " " + userID + " log in failed");
            }

            while(true) {
                System.out.println(" ");
                System.out.println("Please Select An Operation: ");
                System.out.println("1: BorrowItem");
                System.out.println("2: FindItem");
                System.out.println("3: ReturnItem");
                System.out.println("4: ListBorrowedItem");
                System.out.println("5: Exit" + "\n");

                Scanner s = new Scanner(System.in);
                int input = s.nextInt();

                switch (input) {
                    case 1:
                        System.out.println("Enter The ItemID");
                        Scanner input1 = new Scanner(System.in);
                        String itemID = input1.nextLine();
                        System.out.println("Enter The NumberOfDays");
                        Scanner input2 = new Scanner(System.in);
                        int days = input2.nextInt();
                        String itemCampus = itemID.substring(0,3);
                        String userAction = " User ["+ userID + "] borrow item ["+itemID+"] for ["+days+"] days ---> ";
                        String result = h.borrowItem(itemCampus, userID, itemID, days);
                        if(!result.isEmpty()){
                            System.out.println(userAction+"Success.");
                            Log(userID, getFormatDate() + userAction+"Success.");
                        }else{
                            System.out.println(userAction+"Failed.");
                            Log(userID, getFormatDate() + userAction+"Failed.");
                            System.out.println("Do You Want To Wait In The Queue?(Y/N)");
                            Scanner input5 = new Scanner(System.in);
                            String response = input5.nextLine();
                            String userAction2 = " User ["+ userID + "] wait in queue for item ["+itemID+"] ---> ";
                            if(response.equalsIgnoreCase("y")){
                                String waitCampus = itemID.substring(0,3);
                                String waitResult = h.waitInQueue(waitCampus, userID, itemID);
                                if(waitResult.equals(" ")){
                                    System.out.println(userAction2+"Failed.");
                                    Log(userID, getFormatDate() + userAction2+"Failed. ");
                                }else{
                                    System.out.println(userAction2+"Success. Position In Queue :"+waitResult);
                                    Log(userID, getFormatDate() + userAction2+"Success. Position In Queue :"+waitResult);
                                }
                            }
                        }
                        break;

                    case 2:
                        System.out.println("Enter the ItemName");
                        Scanner input3 = new Scanner(System.in);
                        String itemName = input3.nextLine();

                        String userAction3 = " User ["+ userID + "] find item ["+itemName+"] ---> ";
                        String findResult = h.findItem(userID,itemName);
                        if(findResult.isEmpty()){
                            System.out.println(userAction3+ "Failed ");
                            Log(userID, getFormatDate() + " " + userAction3+ "Failed ");
                        }
                        else {
                            System.out.println(userAction3+ "Success. All items: " + findResult);
                            Log(userID, getFormatDate() +userAction3+ "Success. All items: " + findResult);
                        }
                        break;

                    case 3:
                        System.out.println("Enter ItemID To Be Returned:");
                        Scanner input4 = new Scanner(System.in);
                        String returnItemID = input4.nextLine();
                        String returnCampus = returnItemID.substring(0,3);
                        String userAction4 = " User ["+ userID + "] return item ["+returnItemID+"] ---> ";
                        String returnResult = h.returnItem(returnCampus, userID, returnItemID);
                        if(!returnResult.isEmpty()){
                            System.out.println(userAction4+"Success");
                            Log(userID, getFormatDate() + " " + userAction4 +" Success. ");
                        }
                        else {
                            System.out.println(userAction4+"Failed ");
                            Log(userID, getFormatDate() + " " + userAction4 + "Failed ");
                        }
                        break;

                    case 4:
                        String listResult = h.listBorrowedItem(userID);
                        System.out.println("All Borrowed Items : " + listResult);
                        break;

                    case 5:
                        System.exit(5);

                    default:
                        break;
                }
            }
        }
        catch (Exception e) {
            System.out.println("Exception in UserClient: " + e);
        }

    }

}
