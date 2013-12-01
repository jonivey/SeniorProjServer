import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.jonivey.gameplay.Battle;
import com.jonivey.gameplay.Player;
import com.jonivey.messages.AccountCreatedMessage;
import com.jonivey.messages.AccountCreationErrorMessage;
import com.jonivey.messages.AlreadyHasChallengeMessage;
import com.jonivey.messages.BattleCreatedMessage;
import com.jonivey.messages.BattleCreationErrorMessage;
import com.jonivey.messages.BattleMessage;
import com.jonivey.messages.IncorrectPasswordMessage;
import com.jonivey.messages.Message;
import com.jonivey.messages.PasswordTooLongMessage;
import com.jonivey.messages.PlayerDoesNotExistMessage;
import com.jonivey.messages.PlayerHasBattleMessage;
import com.jonivey.messages.PlayerMessage;
import com.jonivey.messages.SuccessfulChallengeMessage;
import com.jonivey.messages.SuccessfulDeclineMessage;
import com.jonivey.messages.SuccessfulSubmitGameoverMessage;
import com.jonivey.messages.SuccessfulUpdateLevelsMessage;
import com.jonivey.messages.SuccessfulUpdateMessage;
import com.jonivey.messages.UnsuccessfulUpdateMessage;
import com.jonivey.messages.UsernameTakenMessage;
import com.jonivey.messages.UsernameTooLongMessage;


public class DatabaseHandler
{
	public DatabaseHandler()
	{
		
	}

	public Message addUser(String name, String password) throws Exception
	{
		Message outputMessage = null;
		
		if(name.length() > 12)
		{
			System.out.println("Username \"" + name + "\" is too long.");
			return new UsernameTooLongMessage(name);
		}
		
		if(password.length() > 20)
		{
			System.out.println("Password \"" + password + "\" is too long.");
			return new PasswordTooLongMessage(password);
		}
		
		name = name.replaceAll("[^A-Za-z0-9]", "");
		
		int exp, str, vit, mob, affected;
		String challenger = null;
		
		exp = 0;
		str = 1;
		vit = 1;
		mob = 1;
		affected = 0;
		
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement pstmt = null;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
			
			ps = conn.prepareStatement("SELECT * from players WHERE username = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			
			if(rs.next())
			{
				System.out.println("Username \"" + name + "\" is taken.");
				outputMessage = new UsernameTakenMessage(name);
			}
			
			else
			{
				pstmt = conn.prepareStatement("INSERT INTO players(player_id,username,password,experience,vitality_level,strength_level,mobility_level,challenge) VALUES(?, ?, ?, ?, ?, ?, ?, ?);");
				pstmt.setInt(1, getNumPlayers() + 1);
				pstmt.setString(2, name);
				pstmt.setString(3, password);
				pstmt.setInt(4, exp);
				pstmt.setInt(5, vit);
				pstmt.setInt(6, str);
				pstmt.setInt(7, mob);
				pstmt.setString(8, challenger);
				
				affected = pstmt.executeUpdate();
					
				if(affected == 1)
				{
					System.out.println("Added \"" + name + "\".");
					outputMessage = new AccountCreatedMessage(name);
				}
				
				else
				{
					System.out.println("Could not add \"" + name + "\".");
					outputMessage = new AccountCreationErrorMessage(name);
				}
			}
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				pstmt = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return outputMessage;
	}

	public Message getUser(String name, String password) throws Exception
	{
		name = name.replaceAll("[^A-Za-z0-9]", "");
		
		int key, exp, str, vit, mob, battle;
		String user, pass, challenge;
		Message outputMessage = null;
		
		exp = 0;
		str = 1;
		vit = 1;
		mob = 1;
		
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement pstmt = null;
		
		if(authenticate(name, password))
		{
			try
			{
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
				
				ps = conn.prepareStatement("SELECT * from players WHERE username = ?");
				ps.setString(1, name);
				ResultSet rs = ps.executeQuery();
				
				if(rs.next())
				{
					key = rs.getInt(1);
					user = rs.getString(2);
					pass = rs.getString(3);
					exp = rs.getInt(4);
					vit = rs.getInt(5);
					str = rs.getInt(6);
					mob = rs.getInt(7);
					challenge = rs.getString(8);
					battle = rs.getInt(9);

					Battle b = getBattle(user, pass);

					outputMessage = new PlayerMessage(user, pass, exp, vit, str, mob, challenge, b);
					System.out.println(key + "\t" + user + "\t" + pass + "\t" + exp + "\t" + vit + "\t" + str + "\t" + mob + "\t" + challenge + "\t" + battle);
				}
				else
				{
					outputMessage = new PlayerDoesNotExistMessage(name);
					String output = "Username: " + name + " does not exist.";
					System.out.println(output);
				}
			}
			
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			finally
			{
				if(pstmt != null)
				{
					try
					{
						pstmt.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					pstmt = null;
				}
				
				if(conn != null)
				{
					try
					{
						conn.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					conn = null;
				}
			}
		}
		
		else
		{
			outputMessage = new IncorrectPasswordMessage(name);
			String output = "Incorrect password.";
			System.out.println(output);
		}
		
		return outputMessage;
	}
	
	public Player getPlayer(String name) throws Exception
	{
		name = name.replaceAll("[^A-Za-z0-9]", "");
		
		int exp, str, vit, mob;
		String user;
		
		exp = 0;
		str = 1;
		vit = 1;
		mob = 1;
		
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement pstmt = null;
		Player p = null;
		
			try
			{
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
				
				ps = conn.prepareStatement("SELECT * from players WHERE username = ?");
				ps.setString(1, name);
				ResultSet rs = ps.executeQuery();
				
				if(rs.next())
				{
					user = rs.getString(2);
					exp = rs.getInt(4);
					vit = rs.getInt(5);
					str = rs.getInt(6);
					mob = rs.getInt(7);
					
					p = new Player(user, exp, vit, str, mob, 0);
				}
			}
			
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			finally
			{
				if(pstmt != null)
				{
					try
					{
						pstmt.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					pstmt = null;
				}
				
				if(conn != null)
				{
					try
					{
						conn.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					conn = null;
				}
			}
		
		return p;
	}
	
	public int getUserID(String name, String password) throws Exception
	{
		name = name.replaceAll("[^A-Za-z0-9]", "");
		
		int key = -1;
		
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement pstmt = null;
		
		if(authenticate(name, password))
		{
			try
			{
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
				
				ps = conn.prepareStatement("SELECT * from players WHERE username = ?");
				ps.setString(1, name);
				ResultSet rs = ps.executeQuery();
				
				if(rs.next())
				{
					key = rs.getInt(1);
				}
			}
			
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			finally
			{
				if(pstmt != null)
				{
					try
					{
						pstmt.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					pstmt = null;
				}
				
				if(conn != null)
				{
					try
					{
						conn.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					conn = null;
				}
			}
		}
		
		return key;
	}
	
	public int getBattleID(String name) throws Exception
	{
		name = name.replaceAll("[^A-Za-z0-9]", "");
		
		int battleID = -1;
		
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement pstmt = null;

			try
			{
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
				
				ps = conn.prepareStatement("SELECT * from players WHERE username = ?");
				ps.setString(1, name);
				ResultSet rs = ps.executeQuery();
				
				if(rs.next())
				{
					battleID = rs.getInt(9);
				}
			}
			
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			finally
			{
				if(pstmt != null)
				{
					try
					{
						pstmt.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					pstmt = null;
				}
				
				if(conn != null)
				{
					try
					{
						conn.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					conn = null;
				}
			}
		
		return battleID;
	}
	
	public String displayUsers() throws Exception
	{
		String output = "";
		Connection conn = null;
		PreparedStatement ps = null;
		String user, pass, challenge;
		int key, exp, vit, str, mob, battle;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
		
			ps = conn.prepareStatement("SELECT * FROM players");
			ResultSet rs = ps.executeQuery();
			
			while(rs.next())
			{
				key = rs.getInt(1);
				user = rs.getString(2);
				pass = rs.getString(3);
				exp = rs.getInt(4);
				vit = rs.getInt(5);
				str = rs.getInt(6);
				mob = rs.getInt(7);
				challenge = rs.getString(8);
				battle = rs.getInt(9);
				output += key + "\t" + user + "\t" + pass + "\t" + exp + "\t" + vit + "\t" + str + "\t" + mob + "\t" + challenge + "\t" + battle + "\n";
			}
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return output;
	}
	
	public Message addBattle(PlayerMessage msg) throws Exception
	{
		Message outputMessage = null;
		
		int affected = 0, battle_id1 = -1, battle_id2 = -1, newBattleID;
		
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement pstmt = null;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
			
			ps = conn.prepareStatement("SELECT * from players WHERE username = ?");
			ps.setString(1, msg.getUsername());
			ResultSet rs1 = ps.executeQuery();
			ps = conn.prepareStatement("SELECT * from players WHERE username = ?");
			ps.setString(1, msg.getChallenger());
			ResultSet rs2 = ps.executeQuery();
			
			if(rs1.next())
			{
				battle_id1 = rs1.getInt(9);
			}
			
			if(rs2.next())
			{
				battle_id2 = rs2.getInt(9);
			}
			
			if(battle_id1 > 0)
			{
				System.out.println(msg.getUsername() + " already has a battle.");
				outputMessage = new PlayerHasBattleMessage(msg.getUsername());
			}
			
			else if(battle_id2 > 0)
			{
				System.out.println(msg.getChallenger() + " already has a battle.");
				outputMessage = new PlayerHasBattleMessage(msg.getChallenger());
			}
			
			if(battle_id1 == 0 && battle_id2 == 0)
			{
				pstmt = conn.prepareStatement("INSERT INTO battles(battle_id,player_one,player_one_coins,player_two,player_two_coins,current_player,battle) VALUES(?, ?, ?, ?, ?, ?, ?);");
				
				newBattleID = getNumBattles()+1;
				
				pstmt.setInt(1, newBattleID);
				pstmt.setString(2, msg.getUsername());
				pstmt.setInt(3, 0);
				pstmt.setString(4, msg.getChallenger());
				pstmt.setInt(5, 0);
				pstmt.setString(6, msg.getUsername());
				pstmt.setObject(7, msg.getBattle());
				
				affected = pstmt.executeUpdate();
					
				if(affected == 1)
				{
					System.out.println("Created a battle for " + msg.getUsername() + " and " + msg.getChallenger() + ".");
					outputMessage = new BattleCreatedMessage(msg);
					
					pstmt = conn.prepareStatement("UPDATE players SET battle_id = ?, challenge = ? WHERE username = ?;");
					pstmt.setInt(1, newBattleID);
					pstmt.setString(2, null);
					pstmt.setString(3, msg.getUsername());
					affected = pstmt.executeUpdate();
					
					if(affected == 1)
						System.out.println("Modified battle id for player " + msg.getUsername());
					else
						System.out.println("Could not modify battle id for player " + msg.getUsername());
					
					pstmt = conn.prepareStatement("UPDATE players SET battle_id = ?, challenge = ? WHERE username = ?;");
					pstmt.setInt(1, newBattleID);
					pstmt.setString(2, null);
					pstmt.setString(3, msg.getChallenger());
					affected = pstmt.executeUpdate();
					
					if(affected == 1)
						System.out.println("Modified battle id for player " + msg.getChallenger());
					else
						System.out.println("Could not modify battle id for player " + msg.getChallenger());
					
					msg.setChallenger(null);
				}
				
				else
				{
					System.out.println("Could not create a battle.");
					outputMessage = new BattleCreationErrorMessage(msg.getUsername(), msg.getChallenger());
				}
			}
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				pstmt = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return outputMessage;
	}
	
	public int getNumBattles() throws Exception
	{
		Connection conn = null;
		PreparedStatement ps = null;
		int numBattles = 0;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
		
			ps = conn.prepareStatement("SELECT * FROM battles");
			ResultSet rs = ps.executeQuery();
			
			rs.last();
			numBattles = rs.getRow();
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return numBattles;
	}
	
	public int getNumPlayers() throws Exception
	{
		Connection conn = null;
		PreparedStatement ps = null;
		int numBattles = 0;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
		
			ps = conn.prepareStatement("SELECT * FROM players");
			ResultSet rs = ps.executeQuery();
			
			rs.last();
			numBattles = rs.getRow();
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return numBattles;
	}
	
	public String displayBattles() throws Exception
	{
		String output = "";
		Connection conn = null;
		PreparedStatement ps = null;
		String player1, player2, currentPlayer;
		int battle_id, player1Coins, player2Coins;
		Battle battle;
		byte[] buffer;
		ObjectInputStream ois = null;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
		
			ps = conn.prepareStatement("SELECT * FROM battles");
			ResultSet rs = ps.executeQuery();
			
			while(rs.next())
			{
				battle_id = rs.getInt(1);
				player1 = rs.getString(2);
				player1Coins = rs.getInt(3);
				player2 = rs.getString(4);
				player2Coins = rs.getInt(5);
				currentPlayer = rs.getString(6);
				
				buffer = rs.getBytes(7);
				
				if(buffer != null)
				{
					ois = new ObjectInputStream(new ByteArrayInputStream(buffer));
				}
				
				battle = (Battle)ois.readObject();
				output += battle_id + "\t" + player1 + "\t" + player1Coins + "\t" + player2 + "\t" + player2Coins + "\t" + currentPlayer + "\t" + battle + "\n";
			}
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return output;
	}
	
	public boolean authenticate(String username, String password) throws Exception
	{
		boolean returnValue = false;
		username = username.replaceAll("[^A-Za-z0-9]", "");
		
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement pstmt = null;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
			
			ps = conn.prepareStatement("SELECT * from players WHERE username = ?");
			ps.setString(1, username);
			ResultSet rs = ps.executeQuery();
			
			if(rs.next())
			{
				String pass = rs.getString(3);
				
				if(password.equals(pass))
					returnValue = true;
				else
				{
					System.out.println("Incorrect password for username " + username);
					returnValue = false;
				}
			}
			else
			{
				System.out.println("Username: " + username + " does not exist.");
				returnValue = false;
			}
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				pstmt = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return returnValue;
	}
	
	public Battle getBattle(String username, String password) throws Exception
	{
		Connection conn = null;
		PreparedStatement ps = null;
		Battle battle = null;
		byte[] buffer;
		ObjectInputStream ois = null;
		int user_id, temp_battle_id;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
		
			user_id = getUserID(username, password);
			
			if(user_id > 0)
			{
				ps = conn.prepareStatement("SELECT * FROM players WHERE player_id = ?;");
				ps.setInt(1, user_id);
				ResultSet rs = ps.executeQuery();
				
				if(rs.next())
				{
					temp_battle_id = rs.getInt(9);
				
					ps = conn.prepareStatement("SELECT * FROM battles WHERE battle_id = ?;");
					ps.setInt(1, temp_battle_id);
					rs = ps.executeQuery();
					
					while(rs.next())
					{	
						buffer = rs.getBytes(7);
						
						if(buffer != null)
						{
							ois = new ObjectInputStream(new ByteArrayInputStream(buffer));
						}
						
						battle = (Battle)ois.readObject();
					}
				}
			}
			
			else
			{
				System.out.println("Player does not exist.");
			}
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return battle;
	}
	
	public Message getBattleMessage(String username, String password) throws Exception
	{
		Connection conn = null;
		PreparedStatement ps = null;
		Battle battle = null;
		byte[] buffer;
		ObjectInputStream ois = null;
		int user_id, temp_battle_id;
		Message outputMessage = null;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
		
			user_id = getUserID(username, password);
			
			if(user_id > 0)
			{
				ps = conn.prepareStatement("SELECT * FROM players WHERE player_id = ?;");
				ps.setInt(1, user_id);
				ResultSet rs = ps.executeQuery();
				
				if(rs.next())
				{
					temp_battle_id = rs.getInt(9);
				
					ps = conn.prepareStatement("SELECT * FROM battles WHERE battle_id = ?;");
					ps.setInt(1, temp_battle_id);
					rs = ps.executeQuery();
					
					while(rs.next())
					{	
						buffer = rs.getBytes(7);
						
						if(buffer != null)
						{
							ois = new ObjectInputStream(new ByteArrayInputStream(buffer));
						}
						
						battle = (Battle)ois.readObject();
						outputMessage = new BattleMessage(battle);
					}
				}
			}
			
			else
			{
				System.out.println("Player does not exist.");
				outputMessage = new PlayerDoesNotExistMessage(username);
			}
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return outputMessage;
	}
	
	public void deleteTables() throws Exception
	{
		Connection conn = null;
		PreparedStatement ps = null;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
		
			ps = conn.prepareStatement("DELETE FROM players");
			ps.executeUpdate();
			
			ps = conn.prepareStatement("DELETE FROM battles");
			ps.executeUpdate();
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
	}
	
	public Message updateBattle(PlayerMessage msg) throws Exception
	{
		Message outputMessage = null;
		
		int affected = 0;
		int battleID = getBattleID(msg.getUsername());
		
		Connection conn = null;
		PreparedStatement ps = null;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
			
			ps = conn.prepareStatement("UPDATE battles SET battle = ? WHERE battle_id = ?");
			ps.setObject(1, msg.getBattle());
			ps.setInt(2, battleID);
			affected = ps.executeUpdate();
					
			if(affected == 1)
			{
				System.out.println("Successfully updated the battle for " + msg.getBattle().getPlayerOne().getName() + " and " + msg.getBattle().getPlayerTwo().getName());
				outputMessage = new SuccessfulUpdateMessage(msg);
				
				updateExperience(msg);
			}
			else
			{
				System.out.println("Failed to update the battle for " + msg.getBattle().getPlayerOne().getName() + " and " + msg.getBattle().getPlayerTwo().getName());
				outputMessage = new UnsuccessfulUpdateMessage();
			}
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return outputMessage;
	}
	
	public void updateExperience(PlayerMessage msg) throws Exception
	{	
		int affected = 0;
		
		Connection conn = null;
		PreparedStatement ps = null;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
					
			ps = conn.prepareStatement("UPDATE players SET experience = ? WHERE username = ?;");
			ps.setInt(1, msg.getExperience());
			ps.setString(2, msg.getUsername());
			affected = ps.executeUpdate();
					
			if(affected == 1)
				System.out.println("Updated experience for player " + msg.getUsername());
			else
				System.out.println("Could not update experience for player " + msg.getUsername());
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
	}
	
	public Message updateLevels(PlayerMessage msg) throws Exception
	{
		Message outputMessage = null;
		
		int affected = 0;
		
		Connection conn = null;
		PreparedStatement ps = null;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
					
			ps = conn.prepareStatement("UPDATE players SET experience = ?, vitality_level = ?, strength_level = ?, mobility_level = ? WHERE username = ?;");
			ps.setInt(1, msg.getExperience());
			ps.setInt(2, msg.getVitality());
			ps.setInt(3, msg.getStrength());
			ps.setInt(4, msg.getMobility());
			ps.setString(5, msg.getUsername());
			affected = ps.executeUpdate();
					
			if(affected == 1)
			{
				System.out.println("Updated levels for player " + msg.getUsername());
				outputMessage = new SuccessfulUpdateLevelsMessage(msg);
			}
			else
			{
				System.out.println("Could not update levels for player " + msg.getUsername());
			}
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return outputMessage;
	}
	
	public Message addChallenge(String receiver, String challenger) throws Exception
	{
		Message outputMessage = null;
		
		int affected = 0;
		String challenge;
		
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement pstmt = null;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
				
				ps = conn.prepareStatement("SELECT * from players WHERE username = ?");
				ps.setString(1, receiver);
				ResultSet rs = ps.executeQuery();
				
				if(rs.next())
				{
					challenge = rs.getString(8);
					
					if(challenge == null)
					{
						pstmt = conn.prepareStatement("UPDATE players SET challenge = ? WHERE username = ?;");
						pstmt.setString(1, challenger);
						pstmt.setString(2, receiver);
						affected = pstmt.executeUpdate();
								
						if(affected == 1)
						{
							System.out.println("Successfully added a challenge for player: " + receiver);
							outputMessage = new SuccessfulChallengeMessage();
						}
						else
						{
							System.out.println("Failed to add a challenge for player: " + receiver);
						}
					}
					
					else
					{
						System.out.println("Player: " + receiver + " already has a challenge");
						outputMessage = new AlreadyHasChallengeMessage();
					}
				}
				else
				{
					outputMessage = new PlayerDoesNotExistMessage(receiver);
					String output = "Username: " + receiver + " does not exist.";
					System.out.println(output);
				}
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				pstmt = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return outputMessage;
	}

	public Message removeChallenge(String username) throws Exception
	{
		Message outputMessage = null;
		
		int affected = 0;
		
		Connection conn = null;
		PreparedStatement ps = null;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
					
			ps = conn.prepareStatement("UPDATE players SET challenge = ? WHERE username = ?;");
			ps.setString(1, null);
			ps.setString(2, username);
			affected = ps.executeUpdate();
					
			if(affected == 1)
			{
				System.out.println("Removed challenge for: " + username);
				outputMessage = new SuccessfulDeclineMessage();
			}
			else
			{
				System.out.println("Could not remove challenge for: " + username);
			}
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return outputMessage;
	}

	public Message removeBattle(PlayerMessage playerMessage) throws Exception
	{
		Message outputMessage = null;
		
		int affected = 0, exp = playerMessage.getExperience();
		
		if(playerMessage.getBattle().getWinner().equals(playerMessage.getUsername()))
			exp+=1000;
		
		Connection conn = null;
		PreparedStatement ps = null;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gamedb", "root", "root");
					
			ps = conn.prepareStatement("UPDATE players SET experience = ?, battle_id = ? WHERE username = ?;");
			ps.setInt(1, exp);
			ps.setInt(2, 0);
			ps.setString(3, playerMessage.getUsername());
			affected = ps.executeUpdate();
					
			if(affected == 1)
			{
				playerMessage.setExperience(exp);
				playerMessage.setBattle(null);
				outputMessage = new SuccessfulSubmitGameoverMessage(playerMessage);
				System.out.println("Cleared battle for: " + playerMessage.getUsername());
			}
			else
			{
				System.out.println("Could not clear battle for: " + playerMessage.getUsername());
			}
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				ps = null;
			}
			
			if(conn != null)
			{
				try
				{
					conn.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				conn = null;
			}
		}
		
		return outputMessage;
	}
}
