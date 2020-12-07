package DLMS;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ServerInterface extends Remote {

    boolean managerLogin(String adminID) throws RemoteException;
    String addItem (String managerID, String itemID, String itemName, int quantity) throws RemoteException;
    String listItemAvailability (String managerID) throws RemoteException;
    String removeItem (String managerID, String itemID, int quantity) throws RemoteException;

    boolean userLogin(String studentID) throws RemoteException;
    String borrowItem (String campusName, String userID, String itemID, int numberOfDays) throws RemoteException;
    String waitInQueue(String campusName, String userID, String itemID) throws RemoteException;
    String findItem (String userID, String itemName) throws RemoteException;
    String returnItem(String campusName, String userID,String itemID) throws RemoteException;
    String listBorrowedItem(String userID) throws RemoteException;

}
