package com.qualcomm.vuforia.samples.Network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.qualcomm.vuforia.Prop;
import com.qualcomm.vuforia.samples.libGDX.BaseGame;
import com.qualcomm.vuforia.samples.libGDX.screens.GameScreen;
import com.qualcomm.vuforia.samples.singletons.DataHolder;
import com.qualcomm.vuforia.samples.singletons.PropertiesSingleton;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Vector;

/**
 * Created by Andreas on 2016-03-16.
 */
public class JoinServer extends Thread
{
    protected String dstAdress;
    protected int dstPort;
    protected Socket socket;
    private String msgtake = "msgtake", msgsend = "", error = "No Error", msglog = "";
    private Vector<String> strConv;
    private boolean connected, ready, allready, ballsChosen, islandChosen;
    private static final int SIZE = 1024;
    private byte[] buffer;
    private int reads;
    public User unitUser;
    private Vector<User> playerList;
    BufferedOutputStream bufferedOutputStream;
    BufferedInputStream bufferedInputStream;
    BaseGame app;

    //Constructor
    public JoinServer(String dstAdress, int dstPort, String name, final BaseGame app)
    {
        this.app = app;
        unitUser = new User(name, 0);
        playerList = new Vector<User>();
        this.dstAdress = dstAdress;
        this.dstPort = dstPort;
        //The name is placeholder, you should be able to enter it yourself when it is integrated with the UI.
        unitUser.setId(name);
        connected = false;
        ready = false;
        allready = false;
        ballsChosen = false;
        islandChosen = false;
        strConv = new Vector<String>();
        PropertiesSingleton.getInstance().setGameMode("standard");
    }

    @Override
    public void run()
    {
        //Create buffer.
        buffer = new byte[SIZE];
        //Instantiate the socket and the input/output streams.
        socket = null;
        bufferedOutputStream = null;
        bufferedInputStream = null;
        try
        {
            //Bind the socket to the given address and port.
            socket = new Socket(dstAdress, dstPort);
            try
            {
                socket.setTcpNoDelay(true);
            }catch(SocketException e)
            {
                e.printStackTrace();
                error = "Exception: " + e.toString();
            }
            //Set streams to read from the socket.
            bufferedInputStream = new BufferedInputStream(socket.getInputStream());
            bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
            //socket.setSoTimeout(5000);
            //Set connection state.
            connected = true;
            //Main connection loop.
            while(connected)
            {
                //Clear the receiving vector.
                strConv.clear();
                //Initial condition for setting player name.
                if(unitUser.getId().equals("player"))
                    msgsend = unitUser.id;
                //Send message statement.
                if(connected && !msgsend.equals(""))
                {
                    sendMessage(msgsend);
                    msgsend = "";
                }
                //Read the input stream for data.
                strConv = readData(reads, buffer);
                //Check if the server has closed.
                if(!connected)
                {
                    break;
                }
                //Receive string containing user data, such as ID and score.
                if(strConv.get(0).equals("USER_DATA_INCOMING"))
                {
                    User usr = new User(strConv.get(1), Integer.parseInt(strConv.get(2)));
                    playerList.add(usr);
                    msgsend = "USER_DATA_GOT";
                }
                else if(strConv.get(0).equals("GEM_POSITION_INCOMING"))
                {
                    Gdx.app.log("HEJ!", "Gem position received.");
                    PropertiesSingleton.getInstance().setSpecialCoinPosition(fromString(strConv.get(1)));
                    islandChosen = true;
                }
                else if(strConv.get(0).equals("GEM_POSITION_AND_INDEX"))
                {
                    PropertiesSingleton.getInstance().setSpecialCoinPosition(fromString(strConv.get(1)));
                    PropertiesSingleton.getInstance().setScore(Integer.parseInt(strConv.get(2)),
                            PropertiesSingleton.getInstance().getScore(Integer.parseInt(strConv.get(2))) + 5);
                    if(app.gameScreen != null)
                        app.gameScreen.updateGemPosition(fromString(strConv.get(1)), Integer.parseInt(strConv.get(2)));

                }
                else if(strConv.get(0).equals("POWERUP_POSITION_INCOMING"))
                {
                    PropertiesSingleton.getInstance().setSpecialPowerupPosition(fromString(strConv.get(1)));
                }
                //Check for a name change request.
                else if(strConv.get(0).equals("NAME_CHANGE"))
                {
                    msgsend = "NAME_CHANGE";
                    setJoinName("player");
                }
                //Receive confirmation of a finished user list.
                else if(strConv.get(0).equals("ALL_USERS_SENT"))
                {
                    app.joinServerScreen.updateDisplayedPlayers(playerList.size(), dstAdress);
                    if(ready != true)
                    {
                        ready = true;
                        sendMessage("READY_CHECK");
                    }
                }
                else if(strConv.get(0).equals("SOUND_PROMPT") && app.gameScreen != null)
                {
                    Vector3 pos = fromString(strConv.get(1));
                    app.gameScreen.playCollisionSound(pos, strConv.get(2), strConv.get(3));
                }
                else if(strConv.get(0).equals("ISLAND_VOTE_RESULT"))
                {
                    PropertiesSingleton.getInstance().setChosenIsland(strConv.get(1));
                    sendMessage("ISLAND_GOT");
                }
                else if(strConv.get(0).equals("ALL_BALLS_CHOSEN"))
                {
                    for(int idu = 0; idu <= playerList.size(); ++idu)
                    {
                        PropertiesSingleton.getInstance().setChosenBall(idu, strConv.get(idu + 1));
                    }
                    ballsChosen = true;
                }
                //Handle incoming positional data, updating all character positions in the gamescreen.
                else if(strConv.get(0).equals("POSITION_INCOMING") && app.gameScreen != null)
                {
                    Vector<Vector3> rec_pos = new Vector<Vector3>(), rec_rot = new Vector<Vector3>();
                    for(int idv = 1; idv <= playerList.size() + 1; ++idv)
                        rec_pos.add(fromString(strConv.get(idv)));
                    for(int idv = 1 + rec_pos.size(); idv <= playerList.size() + rec_pos.size() + 1; ++idv)
                        rec_rot.add(fromString(strConv.get(idv)));
                    app.gameScreen.updatePositions(rec_pos, rec_rot);
                }
                else if(strConv.get(0).equals("GAME_MODE"))
                {
                    PropertiesSingleton.getInstance().setGameMode(strConv.get(1));
                }
                else if(strConv.get(0).equals("NEW_ROUND"))
                {
                    app.scoreScreen.startNewRound();
                }
                else if(strConv.get(0).equals("GAME_LOADED"))
                {
                    app.gameScreen.play = true;
                    app.gameScreen.loading = false;
                    synchronized (app.gameScreen)
                    {
                        app.gameScreen.notify();
                    }
                }
                else if(strConv.get(0).equals("SCORE_INCOMING"))
                {
                    PropertiesSingleton.getInstance().setScore(
                            Integer.parseInt(strConv.get(1)),
                            Integer.parseInt(strConv.get(2)));
                }
                //If the all clear message is received, start the game.
                else if(strConv.get(0).equals("ALL_READY_NOW"))
                {
                    allready = true;
                    Gdx.app.log("Ready?", "allready now");
                }
                //Otherwise, handle message.
                else if(!strConv.get(0).equals(""))
                {
                    if(unitUser.getId().equals("player"))
                    {
                        setJoinName(strConv.get(0));
                        msgsend = unitUser.getId();
                    }
                    msgtake = "Receiving: " + strConv.get(0);
                }
                else
                {
                    msgtake = "Current name: " + unitUser.getId();
                }
            }
        }catch(UnknownHostException e)
        {
            e.printStackTrace();
            error = "Exception: " + e.toString();
        }catch(IOException e)
        {
            e.printStackTrace();
            error = "Exception: " + e.toString();
        }finally
        {
            //Close all streams and the socket.
            if(socket != null)
            {
                try
                {
                    socket.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                    error = "Exception: " + e.toString();
                }
            }
            if(bufferedInputStream != null)
            {
                try
                {
                    bufferedOutputStream.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                    error += "Exception: " + e.toString() + "\n";
                }
            }
            if(bufferedInputStream != null)
            {
                try
                {
                    bufferedInputStream.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                    error += "Exception: " + e.toString() + "\n";
                }
            }
        }
    }

    //Read incoming data from input stream.
    private Vector<String> readData(int readStatus, byte[] buff)
    {
        Vector<String> msg = new Vector<String>();
        String element = "";
        Boolean allRead = false;
        while(!allRead)
        {
            try
            {
                readStatus = bufferedInputStream.read(buff, 0, SIZE);
                if(readStatus == -1)
                {
                    msgtake = "Server is offline.";
                    connected = false;
                    allRead = true;
                }
                else
                {
                    String temp = new String(buff).trim();
                    Gdx.app.log("HEJ!", "Full message: " + temp);
                    for(int idt = 0; idt < temp.length(); ++idt)
                    {
                        //Stop at logical terminator '/'.
                        if(temp.charAt(idt) == '/')
                        {
                            msg.add(element);
                            allRead = true;
                            break;
                        }
                        //Add element at logical terminator '|'.
                        else if(temp.charAt(idt) == '|')
                        {
                            msg.add(element);
                            element = "";
                        }
                        else
                            element += temp.charAt(idt);
                    }
                }
            }catch(IOException e)
            {
                allRead = true;
                e.printStackTrace();
                error = "Exception: " + e.toString();
            }
        }

        return msg;
    }

    public void setScore(int newScore) {unitUser.setScore(newScore);}
    public String getPlayerId(int index) {return playerList.get(index).getId();}
    public String getUnitUserId() {return unitUser.getId();}
    public String getLog() {return msglog;}
    public String getError() {return error;}
    public String getMsg() {return msgtake;}
    public int getPlayerAmount() {return playerList.size() + 1;}
    public Boolean connected() {return connected;}
    public Boolean getAllReadyState() {return allready;}
    public Boolean getIslandChosenState() {return islandChosen;}
    public Boolean getBallChosenState() {return ballsChosen;}
    public void setJoinName(String id) {unitUser.setId(id);}
    public void sendClickPosVector(Vector3 normVec)
    {
        sendMessage("CLICK_POS_INCOMING|" + normVec.toString());
    }

    public void sendIslandChoice(String choice)
    {
        sendMessage("ISLAND_CHOSEN|" + choice);
    }

    public void sendGameLoaded() {sendMessage("GAME_IS_LOADED");}

    public void sendBallChoice(String choice)
    {
        sendMessage("BALL_CHOSEN|" + choice);
    }

    public void sendRoundCheck() { sendMessage("READY_NEXT_ROUND");}
    //Send message via output stream.
    private void sendMessage(String msg)
    {
        //Send message to server.
        try
        {
            //Add logical terminator to end of string.
            String temp = msg + '/';
            byte[] tempbuf;
            tempbuf = temp.getBytes();
            bufferedOutputStream.write(tempbuf, 0, tempbuf.length);
            bufferedOutputStream.flush();
        }catch(IOException e)
        {
            e.printStackTrace();
            error = "Exception: " + e.toString();
        }
    }



    public void disconnect()
    {
        //Set connected state to false and close the socket.
        connected = false;
        try
        {
            Gdx.app.log("Errorlog", "Closing socket");
            socket.close();
        }catch(IOException e)
        {
            e.printStackTrace();
            error = "Exception: " + e.toString();
        }
    }
    //User class, contains player name and score.
    private class User
    {
        //Constructor
        public User(String _name, int _score)
        {
            setScore(_score);
            setId(_name);
        }

        private int score;
        private String id;

        public String getId() {return id;}
        public int getScore() {return score;}
        public void setScore(int newScore) {score = newScore;}
        public void setId(String newId) {id = newId;}
    }

    public Vector3 fromString (String v) {
        int s0 = v.indexOf(',', 1);
        int s1 = v.indexOf(',', s0 + 1);
        if (s0 != -1 && s1 != -1 && v.charAt(0) == '[' && v.charAt(v.length() - 1) == ']') {
            try {
                float x = Float.parseFloat(v.substring(1, s0));
                float y = Float.parseFloat(v.substring(s0 + 1, s1));
                float z = Float.parseFloat(v.substring(s1 + 1, v.length() - 1));
                return new Vector3(x, y, z);
            } catch (NumberFormatException ex) {
                // Throw a GdxRuntimeException
            }
        }
        throw new GdxRuntimeException("Malformed Vector3: " + v);
    }

}

