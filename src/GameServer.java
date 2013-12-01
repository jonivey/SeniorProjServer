import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.jonivey.buildings.Building;
import com.jonivey.gameplay.Battle;
import com.jonivey.gameplay.Cell;
import com.jonivey.gameplay.Map;
import com.jonivey.gameplay.Player;
import com.jonivey.messages.CreateAccountMessage;
import com.jonivey.messages.EndTurnMessage;
import com.jonivey.messages.GetBattleMessage;
import com.jonivey.messages.LoginMessage;
import com.jonivey.messages.Message;
import com.jonivey.messages.SendChallengeMessage;
import com.jonivey.messages.UpdateLevelsMessage;
import com.jonivey.messages.*;
import com.jonivey.units.*;
import com.jonivey.terrain.*;

/**
 * This class contains the server code for my final project.
 * The server code should be able to interact with a MySQL database, as well as
 * handle and validate client requests.
 * @author Jon
 */
public class GameServer
{
	private DatabaseHandler handler;
	
	public GameServer()
	{
		handler = new DatabaseHandler();
	}
	
	public static void main(String[] args) throws Exception
	{
		GameServer server = new GameServer();
		ServerSocket sock = new ServerSocket(31037);
		
		try
		{
			while(true)
			{
				Socket client = sock.accept();
				System.out.println("Accepted a client");
				System.out.println(client.getInetAddress());
				
				InputStream in = client.getInputStream();
				ObjectInputStream objIn = new ObjectInputStream(in);
				System.out.println("Setup input streams");
				
				OutputStream out = client.getOutputStream();
				ObjectOutputStream objOut = new ObjectOutputStream(out);
				System.out.println("Setup output streams");
				
				Message msg = (Message)objIn.readObject();
				System.out.println("Read a message");
				
				if(msg instanceof LoginMessage)
				{
					LoginMessage castedMsg = (LoginMessage)msg;
					System.out.println("Read a LoginMessage");
					Message outputMessage = server.login(castedMsg);
					
					objOut.writeObject(outputMessage);
					objOut.flush();
				}
				else if(msg instanceof CreateAccountMessage)
				{
					CreateAccountMessage castedMsg = (CreateAccountMessage)msg;
					System.out.println("Read a CreateAccountMessage");
					Message outputMessage = server.createNewUser(castedMsg);
					
					objOut.writeObject(outputMessage);
					objOut.flush();
				}
				else if(msg instanceof SendChallengeMessage)
				{
					SendChallengeMessage castedMsg = (SendChallengeMessage)msg;
					System.out.println("Read a SendChallengeMessage: " + castedMsg.getChallenger() + " challenged " + castedMsg.getOpponent());
					
					Message outputMessage = server.handler.addChallenge(castedMsg.getOpponent(), castedMsg.getChallenger());
					
					objOut.writeObject(outputMessage);
					objOut.flush();
				}
				else if(msg instanceof DeclineChallengeMessage)
				{
					DeclineChallengeMessage castedMsg = (DeclineChallengeMessage)msg;
					System.out.println("Read a DeclineChallengeMessage from: " + castedMsg.getUsername());
					
					Message outputMessage = server.handler.removeChallenge(castedMsg.getUsername());
					
					objOut.writeObject(outputMessage);
					objOut.flush();
				}
				else if(msg instanceof AcceptChallengeMessage)
				{
					AcceptChallengeMessage castedMsg = (AcceptChallengeMessage)msg;
					System.out.println("Read an AcceptChallengeMessage: " + castedMsg.getPlayerMessage().getUsername() + " accepted " + castedMsg.getPlayerMessage().getChallenger() + "'s challenge");
					
					Player p1 = server.handler.getPlayer(castedMsg.getPlayerMessage().getUsername());
					Player p2 = server.handler.getPlayer(castedMsg.getPlayerMessage().getChallenger());
					
					int numRows = 25, numCols = 25;
					
					Cell[][] cells = new Cell[numRows][numCols];
					
					for(int row = 0; row < numRows; row++)
						for(int col = 0; col < numCols; col++)
						{
							cells[row][col] = new Cell();
							
							if(row == 0 || row == 1 || row == 23 || row == 24 || col == 0 || col == 1 || col == 23 || col == 24)
								cells[row][col] = new Cell(new Terrain(Terrain.TerrainType.WATER));
							else if((row == 6 || row == 18) && col >= 11 && col <= 13)
								cells[row][col] = new Cell(new Terrain(Terrain.TerrainType.WATER));
							else if((row == 7 || row == 17) && col >= 10 && col <= 14)
								cells[row][col] = new Cell(new Terrain(Terrain.TerrainType.WATER));
							else if((row == 8 || row == 9 || row == 15 || row == 16) && col >= 9 && col <= 15)
								cells[row][col] = new Cell(new Terrain(Terrain.TerrainType.WATER));
							else if((row == 10 || row == 11 || row == 13 || row == 14) && col >= 8 && col <= 16)
								cells[row][col] = new Cell(new Terrain(Terrain.TerrainType.WATER));
							else
							{
								if((row + col) % 4 == 0)
									cells[row][col] = new Cell(new Terrain(Terrain.TerrainType.FOREST));
								if((row + col) % 8 == 0)
									cells[row][col] = new Cell(new Terrain(Terrain.TerrainType.MOUNTAIN));
							}
						}
					
					cells[5][12] = new Cell(new Terrain(Terrain.TerrainType.WATER));
					cells[2][11] = new Cell(new Terrain(Terrain.TerrainType.WATER));
					cells[2][12] = new Cell(new Terrain(Terrain.TerrainType.WATER));
					cells[2][13] = new Cell(new Terrain(Terrain.TerrainType.WATER));
					cells[3][12] = new Cell(new Terrain(Terrain.TerrainType.WATER));
					
					cells[19][12] = new Cell(new Terrain(Terrain.TerrainType.WATER));
					cells[21][12] = new Cell(new Terrain(Terrain.TerrainType.WATER));
					cells[22][11] = new Cell(new Terrain(Terrain.TerrainType.WATER));
					cells[22][12] = new Cell(new Terrain(Terrain.TerrainType.WATER));
					cells[22][13] = new Cell(new Terrain(Terrain.TerrainType.WATER));
					
					cells[2][2].setBuilding(new Building(Building.BuildingType.CAPITAL, 1));
					cells[22][22].setBuilding(new Building(Building.BuildingType.CAPITAL, 2));
					
					Unit u1 = new Soldier();
					u1.setOwner(1);
					u1.setAttackPower(u1.getAttackPower() + ((p1.getStrengthLevel() - 1) * 5));
					u1.setCurrentHealth(u1.getCurrentHealth() + ((p1.getVitalityLevel() - 1) * 5));
					u1.setMaximumHealth(u1.getMaximumHealth() + ((p1.getVitalityLevel() - 1) * 5));
					u1.setMovementRange(u1.getMovementRange() + (p1.getMobilityLevel() / 5));
					cells[3][3].setUnit(u1);
					
					Unit u2 = new Soldier();
					u2.setOwner(2);
					u2.setAttackPower(u2.getAttackPower() + ((p2.getStrengthLevel() - 1) * 5));
					u2.setCurrentHealth(u2.getCurrentHealth() + ((p2.getVitalityLevel() - 1) * 5));
					u2.setMaximumHealth(u2.getMaximumHealth() + ((p2.getVitalityLevel() - 1) * 5));
					u2.setMovementRange(u2.getMovementRange() + (p2.getMobilityLevel() / 5));
					cells[21][21].setUnit(u2);
					
					Building factory1 = new Building(Building.BuildingType.FACTORY);
					factory1.setOwner(0);
					Building airport1 = new Building(Building.BuildingType.AIRPORT);
					airport1.setOwner(0);
					Building port1 = new Building(Building.BuildingType.PORT);
					port1.setOwner(0);
					
					Building factory2 = new Building(Building.BuildingType.FACTORY);
					factory2.setOwner(0);
					Building airport2 = new Building(Building.BuildingType.AIRPORT);
					airport2.setOwner(0);
					Building port2 = new Building(Building.BuildingType.PORT);
					port2.setOwner(0);
					
					cells[5][5].setBuilding(factory1);
					cells[2][22].setBuilding(airport1);
					cells[4][12].setBuilding(port1);
					
					cells[19][19].setBuilding(factory2);
					cells[22][2].setBuilding(airport2);
					cells[20][12].setBuilding(port2);
					
					Battle b = new Battle(p1, p2, new Map(cells, numRows, numCols), castedMsg.getPlayerMessage().getUsername());
					castedMsg.getPlayerMessage().setBattle(b);
					
					Message outputMessage = server.createBattle(castedMsg.getPlayerMessage());
					
					objOut.writeObject(outputMessage);
					objOut.flush();
				}
				else if(msg instanceof GetBattleMessage)
				{
					GetBattleMessage castedMsg = (GetBattleMessage)msg;
					Message outputMessage = server.handler.getBattleMessage(castedMsg.getUsername(), castedMsg.getPassword());
					
					objOut.writeObject(outputMessage);
					objOut.flush();
				}
				else if(msg instanceof EndTurnMessage)
				{
					
					EndTurnMessage castedMsg = (EndTurnMessage)msg;
					System.out.println("Read an EndTurnMessage from: " + castedMsg.getPlayerMessage().getUsername());
					
					int buildingCount = 0;
					
					int myTeam;
					
					if(castedMsg.getPlayerMessage().getBattle().getPlayerOne().getName().equals(castedMsg.getPlayerMessage().getUsername()))
						myTeam = 1;
					else
						myTeam = 2;
					
					if(castedMsg.getPlayerMessage().getBattle().getTurn().equals(castedMsg.getPlayerMessage().getBattle().getPlayerOne().getName()))
						castedMsg.getPlayerMessage().getBattle().setTurn(castedMsg.getPlayerMessage().getBattle().getPlayerTwo().getName());
					else
						castedMsg.getPlayerMessage().getBattle().setTurn(castedMsg.getPlayerMessage().getBattle().getPlayerOne().getName());
					
					for(int row = 0; row < castedMsg.getPlayerMessage().getBattle().getMap().getNumRows(); row++)
						for(int col = 0; col < castedMsg.getPlayerMessage().getBattle().getMap().getNumCols(); col++)
						{
							if(castedMsg.getPlayerMessage().getBattle().getMap().getCell(row, col).getUnit() != null)
							{
								castedMsg.getPlayerMessage().getBattle().getMap().getCell(row, col).getUnit().setAttacked(false);
								castedMsg.getPlayerMessage().getBattle().getMap().getCell(row, col).getUnit().setMoved(false);
							}
							
							if(castedMsg.getPlayerMessage().getBattle().getMap().getCell(row, col).getBuilding() != null)
							{
								if(castedMsg.getPlayerMessage().getBattle().getMap().getCell(row, col).getBuilding().getOwner() == myTeam)
									buildingCount++;
							}
						}
					
					if(myTeam == 1)
						castedMsg.getPlayerMessage().getBattle().getPlayerOne().setCoins(castedMsg.getPlayerMessage().getBattle().getPlayerOne().getCoins() + 200*buildingCount);
					else
						castedMsg.getPlayerMessage().getBattle().getPlayerTwo().setCoins(castedMsg.getPlayerMessage().getBattle().getPlayerTwo().getCoins() + 200*buildingCount);
					
					Message outputMessage = server.handler.updateBattle(castedMsg.getPlayerMessage());
					
					objOut.writeObject(outputMessage);
					objOut.flush();
				}
				else if(msg instanceof UpdateLevelsMessage)
				{
					UpdateLevelsMessage castedMsg = (UpdateLevelsMessage)msg;
					System.out.println("Read an UpdateLevelsMessage from: " + castedMsg.getPlayerMessage().getUsername());
					
					Message outputMessage = server.handler.updateLevels(castedMsg.getPlayerMessage());
					
					objOut.writeObject(outputMessage);
					objOut.flush();
				}
				else if(msg instanceof ResignMessage)
				{
					ResignMessage castedMsg = (ResignMessage)msg;
					System.out.println("Read a ResignMessage from: " + castedMsg.getPlayerMessage().getUsername());
					
					castedMsg.getPlayerMessage().getBattle().setGameOver(true);
					
					System.out.println(castedMsg.getPlayerMessage().getUsername() + " is resigning");
					
					if(castedMsg.getPlayerMessage().getUsername().equals(castedMsg.getPlayerMessage().getBattle().getPlayerOne().getName()))
						castedMsg.getPlayerMessage().getBattle().setWinner(castedMsg.getPlayerMessage().getBattle().getPlayerTwo().getName());
					else
						castedMsg.getPlayerMessage().getBattle().setWinner(castedMsg.getPlayerMessage().getBattle().getPlayerOne().getName());
					
					System.out.println("Winner: " + castedMsg.getPlayerMessage().getBattle().getWinner());
					
					Message outputMessage = server.handler.updateBattle(castedMsg.getPlayerMessage());
					
					objOut.writeObject(outputMessage);
					objOut.flush();
				}
				else if(msg instanceof SubmitGameoverMessage)
				{
					SubmitGameoverMessage castedMsg = (SubmitGameoverMessage)msg;
					System.out.println("Read a SubmitGameoverMessage from: " + castedMsg.getPlayerMessage().getUsername());
					
					if(castedMsg.getPlayerMessage().getBattle().getTurn().equals(castedMsg.getPlayerMessage().getBattle().getPlayerOne().getName()))
						castedMsg.getPlayerMessage().getBattle().setTurn(castedMsg.getPlayerMessage().getBattle().getPlayerTwo().getName());
					else
						castedMsg.getPlayerMessage().getBattle().setTurn(castedMsg.getPlayerMessage().getBattle().getPlayerOne().getName());
					
					Message outputMessage = server.handler.updateBattle(castedMsg.getPlayerMessage());
					
					if(outputMessage instanceof SuccessfulUpdateMessage)
						outputMessage = server.handler.removeBattle(castedMsg.getPlayerMessage());
					
					objOut.writeObject(outputMessage);
					objOut.flush();
				}
				else
				{
					System.out.println("error reading object");
				}
				
				objIn.close();
				in.close();
				objOut.close();
				out.close();
				client.close();
				System.out.println("Client closed");
			}
		}
		
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			sock.close();
		}
	}
	
	/*private void deleteTables()
	{
		try
		{
			handler.deleteTables();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private String displayBattles()
	{
		String output = null;
		
		try
		{
			output = handler.displayBattles();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return output;
	}*/

	public Message createBattle(PlayerMessage msg)
	{
		Message returnedMessage = null;
		
		try
		{
			returnedMessage = handler.addBattle(msg);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return returnedMessage;
	}

	/*private String displayUsers()
	{
		String output = null;
		
		try
		{
			output = handler.displayUsers();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return output;
	}*/

	public Message createNewUser(CreateAccountMessage msg)
	{
		Message returnedMessage = null;
		
		String name = msg.getUsername();
		name.replaceAll("[^A-Za-z0-9]", "");
		String password = msg.getPassword();
		try
		{
			returnedMessage = handler.addUser(name, password);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return returnedMessage;
	}
	
	public Message login(LoginMessage msg)
	{
		Message returnedMessage = null;
		try
		{
			returnedMessage = handler.getUser(msg.getUsername(), msg.getPassword());
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return returnedMessage;
	}
}
