import java.net.*;
import java.sql.*;
import java.io.*;


public class TTPServer implements Runnable {
   Socket sock= null;
   String username;
   String password;
   int providerId;
   String user;
   String pass;

   public TTPServer(Socket _sock, String _user, String _pass) {
      sock = _sock;
	  user=_user;
	  pass=_pass;
   }

   public void run() {
      try {
         BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
         String request = in.readLine();
		 if(request == null) {
			sock.getOutputStream().write("Authentication failed, must provide username, password, providerId".getBytes());
			return;
		 }
         System.out.println(request);
		 
		  Class.forName("com.mysql.jdbc.Driver").newInstance();
          Connection con = DriverManager.getConnection("jdbc:mysql://cs.okstate.edu:3306/"+user, user, pass);
		 
		 PreparedStatement query = makePasswordQuery(request, con);
		 if(query==null) {
			sock.getOutputStream().write("Authentication failed, must provide username, password, providerId".getBytes());
		 }
		 System.out.println(query);
		 
		 ResultSet rs = query.executeQuery();
		 if(rs.next()==false) {
			sock.getOutputStream().write("Authentication failed, cannot get user".getBytes());
			return;
		 }
         String dbPassword = rs.getString("password");
         int userId = rs.getInt("id");
		 
		 if(password.equals(dbPassword)) {
			query = makeTokenQuery(userId, con);
			System.out.println(query);
		 } else {
		    sock.getOutputStream().write("Authentication failed, bad password".getBytes());
			return;
		 }

         rs = query.executeQuery();
         if(rs.next()==false) {
			sock.getOutputStream().write("Authentication failed, bad provider id".getBytes());
			return;
		 }
         String _userKey = rs.getString("userKey");
         System.out.println(userId + "\t" + providerId + "\t" + _userKey);

         con.close();

         sock.getOutputStream().write(_userKey.getBytes());
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         try {
            sock.close();
         } catch (IOException e1) {
            e1.printStackTrace();
            sock=null;
         }
      }
   }

   private PreparedStatement makePasswordQuery(String request, Connection con) throws SQLException {
      String[] fields = request.split(",");
	  if(fields ==null || fields.length!=3) {
		return null;
	  }
      username = fields[0];
	  password = fields[1];
	  try {
		providerId = Integer.parseInt(fields[2]);
	  } catch(NumberFormatException e) {
		return null;
	  }
	  
	  PreparedStatement prep = con.prepareStatement("SELECT password, id FROM userData WHERE username = ?");
	  prep.setString(1, username);
	  return prep;
   }
   
    private PreparedStatement makeTokenQuery(int userId, Connection con) throws SQLException {
	  PreparedStatement prep = con.prepareStatement("SELECT userKey FROM userKeys WHERE userId = ? AND providerId = ?");
	  prep.setInt(1, userId);
	  prep.setInt(2, providerId);
	  return prep;
   }
   
   public static void main(String[] args) {
      ServerSocket ss = null;

      try {
         ss = new ServerSocket(8080);
      }
      catch (IOException e) {
         e.printStackTrace();
         System.out.println("Unable to create TTP Server");
         return;
      }

      while(true) {
         try {
            Thread t = new Thread(new TTPServer(ss.accept(), args[0], args[1]));
            t.start();
         } catch (IOException e) {
            System.out.println("Unable to accept new connection.");
            try {
               ss.close();
            } catch (IOException e1) {
               e1.printStackTrace();
               ss=null;
            }
         } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
		    System.out.println("java TTPServer <db username> <password>");
		 }
      }
   }
}
