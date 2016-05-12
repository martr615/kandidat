package com.qualcomm.vuforia.samples.libGDX.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.ContactCache;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btConvexHullShape;
import com.badlogic.gdx.physics.bullet.collision.btPersistentManifold;
import com.badlogic.gdx.physics.bullet.collision.btShapeHull;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.qualcomm.vuforia.samples.libGDX.BaseGame;
import com.qualcomm.vuforia.samples.libGDX.bullet.BaseBulletTest;
import com.qualcomm.vuforia.samples.libGDX.bullet.BaseWorld;
import com.qualcomm.vuforia.samples.libGDX.bullet.BulletConstructor;
import com.qualcomm.vuforia.samples.libGDX.bullet.BulletEntity;
import com.qualcomm.vuforia.samples.libGDX.classes.Coin;
import com.qualcomm.vuforia.samples.libGDX.classes.GameSound;
import com.qualcomm.vuforia.samples.libGDX.classes.Player;
import com.qualcomm.vuforia.samples.singletons.DataHolder;
import com.qualcomm.vuforia.samples.singletons.PropertiesSingleton;

import java.util.Collections;
import java.util.Vector;

public class GameScreen extends BaseBulletTest implements Screen {

    //public AssetManager assets;
    Vector<BulletEntity> playerEntityList = new Vector<BulletEntity>();
    Vector<Player> playerList = new Vector<Player>();
    private Stage stage;
    private Stage scoreStage;

    // App reference
    BaseGame app;

    // UI
    private Label LabelScorePlayer1,LabelScorePlayer2,LabelScorePlayer3, LabelScorePlayer4;
    private Label.LabelStyle labelStyle;

    // Stages

    int thisUnitId;

    // Game related variables
    float gameOverTimer = 0;
    public float scoreTimer;

    boolean collisionHappened = false;
    boolean gameOverGameScreen = false;
    boolean playerCreated = false;
    boolean loading = false;


    // Control
    private ClosestRayResultCallback rayTestCB;
    private Vector3 rayFrom = new Vector3();
    private Vector3 rayTo = new Vector3();


    private ModelInstance instance;

    // Score lables

    public static float time;

    private boolean remove = false;
    final boolean USE_CONTACT_CACHE = true;
    TestContactCache contactCache;
    BulletEntity bomb1;
    public Coin coin1;
    //public Player player_1, player_2, player_3;

    // Sound
    static GameSound gameSound;
    int collisionUserId0, collisionUserId1;

    //countdown
    private Label LabelCountdown;
    private Label.LabelStyle labelStyleCountdown;
    private float totalTime = 3;
    boolean countdownFinished = false;

    AssetManager assets;
    String chosenIsland;
    String chosenBall;
    BulletEntity player;
    Model arrowInstance;
    Label labelTitle;
    private AnimationController controller;


    public GameScreen(final BaseGame app)
    {

        this.app = app;
        this.assets = PropertiesSingleton.getInstance().getAssets();
        if(app.createServerScreen.create != null)
            thisUnitId = 0;
        else if(app.joinServerScreen.join != null)
            thisUnitId = (Character.getNumericValue(app.joinServerScreen.join.getUnitUserId().charAt(
                    app.joinServerScreen.join.getUnitUserId().length() - 1)) - 1);
        this.chosenIsland = PropertiesSingleton.getInstance().getChosenIsland();
        this.chosenBall = PropertiesSingleton.getInstance().getChosenBall(thisUnitId);
        this.create();

    }

    public class TestContactCache extends ContactCache {
        public Array<BulletEntity> entities;
        @Override
        public void onContactStarted (btPersistentManifold manifold, boolean match0, boolean match1) {
            final int userValue0 = manifold.getBody0().getUserValue();
            final int userValue1 = manifold.getBody1().getUserValue();

            // Take the positions of the colliding balls. Used in the handling of sounds.
            Vector3 p1 = ((btRigidBody) manifold.getBody0()).getCenterOfMassPosition();
            Vector3 p2 = ((btRigidBody) manifold.getBody1()).getCenterOfMassPosition();

            // Set the time which the player1 can receive a points after a collision has happened.
            // 1 second = 30f
            scoreTimer = 210f;  // 210/30 = 7 seconds
            collisionHappened = true;

            if((entities.get(userValue0) != entities.get(0))){
            if (entities.get(userValue0) == entities.get(1) || entities.get(userValue1) == entities.get(1)) {
                    if (match0) {
                        final BulletEntity e = (BulletEntity) (entities.get(userValue0));
                        e.setColor(Color.BLUE);
                        Gdx.app.log(Float.toString(time), "Contact started 0 " + userValue0);
                        collisionUserId0 = userValue0;
                    }
                    if (match1) {
                        final BulletEntity e = (BulletEntity) (entities.get(userValue1));
                        e.setColor(Color.RED);
                        Gdx.app.log(Float.toString(time), "Contact started 1 " + userValue1);
                        collisionUserId1 = userValue1;
                    }
                    // Play the collision sound.
//                    gameSound.playCollisionSound(p1, p2);
            }
         }
        }

        @Override
        public void onContactEnded (btCollisionObject colObj0, boolean match0, btCollisionObject colObj1, boolean match1) {
            final int userValue0 = colObj0.getUserValue();
            final int userValue1 = colObj1.getUserValue();

            if (entities.get(userValue0) == entities.get(1)|| entities.get(userValue1) == entities.get(1)) {
                if (match0) {
                    final BulletEntity e = (BulletEntity) (entities.get(userValue0));
                    e.setColor(Color.BLACK);
                    Gdx.app.log(Float.toString(time), "Contact ended " + collisionUserId1);
                }
                if (match1) {
                    final BulletEntity e = (BulletEntity) (entities.get(userValue1));
                    e.setColor(Color.BLACK);
                    Gdx.app.log(Float.toString(time), "Contact ended " + collisionUserId0);
                }
            }
        }
    }

    @Override
    public void create () {
        super.create();

        final Model island = assets.get("3d/islands/"+chosenIsland+".g3db", Model.class);
        disposables.add(island);
        final BulletConstructor sceneConstructor = new BulletConstructor(island, 0f, new btBvhTriangleMeshShape(
                island.meshParts));
        sceneConstructor.bodyInfo.setRestitution(0.25f);
        world.addConstructor("island", sceneConstructor);
        world.add("island", 0, 0, 0);


        arrowInstance = assets.get("3d/misc/"+"arrow"+".g3db", Model.class);
//        arrowInstance.meshes.get(0).scale(0.1f, 0.1f, 0.1f);


        // Setup the stages
        this.stage = new Stage(new StretchViewport(Gdx.graphics.getHeight(), Gdx.graphics.getHeight()));
        this.scoreStage = new Stage(new StretchViewport(Gdx.graphics.getHeight(), Gdx.graphics.getHeight()));

        Gdx.app.log("SHOOT", "Begin");

        // Create font

        // Init Score lables
        labelStyle = new Label.LabelStyle(app.font40, Color.PINK);

        LabelScorePlayer1 = new Label("", labelStyle);
        LabelScorePlayer1.setPosition(Gdx.graphics.getWidth()*0.01f, Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 20)*1);
        LabelScorePlayer2 = new Label("", labelStyle);
        LabelScorePlayer2.setPosition(Gdx.graphics.getWidth()*0.01f, Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 20)*2);
        LabelScorePlayer3 = new Label("", labelStyle);
        LabelScorePlayer3.setPosition(Gdx.graphics.getWidth()*0.01f, Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 20)*3);
        LabelScorePlayer4 = new Label("", labelStyle);
        LabelScorePlayer4.setPosition(Gdx.graphics.getWidth()*0.01f, Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 20)*4);

        stage.addActor(LabelScorePlayer1);
        stage.addActor(LabelScorePlayer2);
        stage.addActor(LabelScorePlayer3);
        stage.addActor(LabelScorePlayer4);

        Label.LabelStyle labelStyle = new Label.LabelStyle(app.font120, Color.WHITE);
        labelTitle = new Label("NOT TRACKING ", labelStyle);
        labelTitle.setPosition(Gdx.graphics.getHeight() / 2 - labelTitle.getWidth() / 2, Gdx.graphics.getHeight() - labelTitle.getHeight() * 2);

        stage.addActor(labelTitle);

        Actor scoreActor = new Image(new Sprite(new Texture(Gdx.files.internal("img/scorebg1.png"))));
        scoreActor.setPosition(0, 0);
        scoreActor.setSize((stage.getWidth()), stage.getHeight());
        scoreStage.addActor(scoreActor);

        scoreStage.getRoot().setPosition(0, stage.getHeight());
        Gdx.input.setInputProcessor(this);

        Model chosenBallModel = assets.get("3d/balls/"+chosenBall+".g3db", Model.class);

        for(int k = 0; k<chosenBallModel.meshes.size;k++)
            chosenBallModel.meshes.get(k).scale(0.2f,0.2f,0.2f);

        float playerPosOffset = 0.0f;
        Gdx.app.log("HEJ!", "Nr of players: " + PropertiesSingleton.getInstance().getNrPlayers());
        int joinOffset = 0;
        for(int idu = 0; idu < PropertiesSingleton.getInstance().getNrPlayers(); ++idu)
        {
            if(app.joinServerScreen.join != null)
            {
                if(idu != Character.getNumericValue(app.joinServerScreen.join.getUnitUserId().charAt(app.joinServerScreen.join.getUnitUserId().length() - 1)) - 1)
                {
                    Gdx.app.log("HEJ!", "Adding other.");
                    playerList.add(new Player(chosenBallModel, app.joinServerScreen.join.getPlayerId(idu - joinOffset)));
                    world.addConstructor("Test " + idu, playerList.get(idu).bulletConstructor);
                    playerEntityList.add(world.add("Test " + idu, 0, 300f, 1.0f + playerPosOffset));
                    playerEntityList.get(idu).body.setContactCallbackFilter(1);
                }
                else
                {
                    Gdx.app.log("HEJ!", "Adding me.");
                    ++joinOffset;
                    Gdx.app.log("HEJ!", "New offset: " + joinOffset);
                    thisUnitId = idu;
                    playerList.add(new Player(chosenBallModel, app.joinServerScreen.join.getUnitUserId()));
                    world.addConstructor("Test " + idu, playerList.get(idu).bulletConstructor);
                    playerEntityList.add(world.add("Test " + idu, 0, 300f, 1.0f + playerPosOffset));
                    playerEntityList.get(idu).body.setContactCallbackFlag(1);
                    playerEntityList.get(idu).body.setContactCallbackFilter(1);
                }
            }
            else if(app.createServerScreen.create != null)
            {
                Gdx.app.log("HEJ!", "Create is not null.");
                thisUnitId = 0;
                if(idu == 0)
                {
                    playerList.add(new Player(chosenBallModel, app.createServerScreen.create.getServerName()));
                    world.addConstructor("Test " + idu, playerList.get(idu).bulletConstructor);
                    playerEntityList.add(world.add("Test " + idu, 0, 300f, 1.0f));
                    playerEntityList.get(idu).body.setContactCallbackFilter(1);
                    playerEntityList.get(idu).body.setContactCallbackFlag(1);
                }
                else
                {
                    playerList.add(new Player(chosenBallModel, app.createServerScreen.create.getUserId(idu - 1)));
                    world.addConstructor("Test " + idu, playerList.get(idu).bulletConstructor);
                    playerEntityList.add(world.add("Test " + idu, 0, 300f, 1.0f + playerPosOffset));
                    playerEntityList.get(idu).body.setContactCallbackFilter(1);
                }
            }
            playerPosOffset += 2;
            Gdx.app.log("HEJ!", "End of loop.");
        }
        playerCreated = true;
        if (USE_CONTACT_CACHE) {
            contactCache = new TestContactCache();
            contactCache.entities = world.entities;
            // contactCache.setCacheTime(contactTime); // Change the contact time
        }
        Gdx.app.log("SHOOT", "END");
        Gdx.app.log("SHOOT", "Singleton: " + PropertiesSingleton.getInstance().getNrPlayers());
        gameSound = new GameSound();
        // Play background music.
        // gameSound.playBackgroundMusic(0.45f);

        //-------------------------load countdown--------------------------
        labelStyleCountdown = new Label.LabelStyle(app.font40, Color.GREEN);
        LabelCountdown = new Label("", labelStyleCountdown);
        LabelCountdown.setPosition(Gdx.graphics.getHeight() / 2, Gdx.graphics.getWidth() / 2);
        stage.addActor(LabelCountdown);
        Gdx.app.log("SHOOT", "Nr of characters: " + playerList.size());
          // Sound
        //-------------------------------------------------------------------
    }

    @Override
    public boolean tap (float x, float y, int count, int button) {
        shoot(x, y);
        Gdx.app.log("TAP", "Tap");
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
       // shoot(screenX, screenY);
        Gdx.app.log("SHOOT", "SHOOT");

        //TO REMOVE AN ENTITY FROM THE WORLD
//        world.remove(playerEntityList.indexOf(player1.body) + 2);

        //LOG THE POSITION OF A BALL
        /*Vector3 tmpVec = new Vector3(0,2,0);
        world.entities.get(1).body.getWorldTransform().setToTranslation(tmpVec);


        Matrix4 m = new Matrix4();
        world.entities.get(1).body.setWorldTransform(m.setToTranslation(tmpVec));*/


        //FÅ ROTATIONEN
//        (((btRigidBody) player1.body).getAngularVelocity();


        //Gdx.app.log("LOG",tmpVec+ "");


        //if(countdownFinished){
        Ray ray = camera.getPickRay(screenX, screenY);
        rayFrom.set(ray.origin);
        rayTo.set(ray.direction).scl(1000f).add(rayFrom); // 50 meters max from the origin

        // Because we reuse the ClosestRayResultCallback, we need reset it's values
        ClosestRayResultCallback rayTestCB = new ClosestRayResultCallback(Vector3.Zero, Vector3.Z);
        rayTestCB.setCollisionObject(null);
        rayTestCB.setClosestHitFraction(1f);
        rayTestCB.setRayFromWorld(rayFrom);
        rayTestCB.setRayToWorld(rayTo);

        world.collisionWorld.rayTest(rayFrom, rayTo, rayTestCB);
        if(rayTestCB.hasHit())
        {
            rayTestCB.getHitPointWorld(tmpV1);

            Gdx.app.log("BANG", "BANG");
//            Model model;
//            ModelBuilder modelBuilder = new ModelBuilder();
//            modelBuilder.begin();
//            modelBuilder.part("ball", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal | Usage.TextureCoordinates,
//                    new Material("diffuseGreen", ColorAttribute.createDiffuse(Color.RED))).sphere(1f, 1f, 1f, 10, 10);
//            model = modelBuilder.end();
//
//            instance = new ModelInstance(model,tmpV1);

            Vector3 vec = new Vector3((tmpV1.x - ((btRigidBody) playerEntityList.get(thisUnitId).body).getCenterOfMassPosition().x),
                                      0, (tmpV1.z - ((btRigidBody) playerEntityList.get(thisUnitId).body).getCenterOfMassPosition().z));


            instance = new ModelInstance(arrowInstance, tmpV1);
            // You use an AnimationController to um, control animations.  Each control is tied to the model instance
            controller = new AnimationController(instance);

            // Pick the current animation by name
            controller.setAnimation("ArrowAction", -1);

            float normFactor = playerList.get(thisUnitId).impulseFactor / vec.len();
            Vector3 normVec = new Vector3(normFactor * vec.x, normFactor * vec.y, normFactor * vec.z);
            if(app.createServerScreen.create != null)
            {
                playerEntityList.get(thisUnitId).body.activate();
                ((btRigidBody) playerEntityList.get(thisUnitId).body).applyCentralImpulse(normVec);
            }
            Gdx.app.log("HEJ!", "Player " + (thisUnitId+1));
            Gdx.app.log("HEJ!", "Normvec: " + normVec.toString());
            if(app.joinServerScreen.join != null)
            {
                app.joinServerScreen.join.sendClickPosVector(normVec);
            }
        }
            // Är det normVec som ska skickas till servern som ´sen skickar till varje client och varje client lägger impulsen på rätt spelare.
            // sendImpulse(normVec);
            // Skriva en ny funktion i GameScreen som faktiskt sätter denna impuls, vart ska den sättas? Vill inte att den ska köras varje frame.
            // Ifall klick har hänt,
        //}
        return true;
    }

    boolean up, down, left, right;
    @Override
    public boolean keyDown (int keycode) {
        /*player2.body.activate();
        Vector3 moveDown = new Vector3(1f, 0f, 0f);
        Vector3 moveUp = new Vector3(-1f, 0f, 0f);
        Vector3 moveLeft = new Vector3(0f, 0f, 1f);
        Vector3 moveRight = new Vector3(0f, 0f, -1f);

        switch(keycode) {
            case Input.Keys.UP: up = true;
                ((btRigidBody) player2.body).applyCentralImpulse(moveUp);
                break;
            case Input.Keys.DOWN: down = true;
                ((btRigidBody) player2.body).applyCentralImpulse(moveDown);
                break;
            case Input.Keys.LEFT: left = true;
                ((btRigidBody) player2.body).applyCentralImpulse(moveLeft);
                break;
            case Input.Keys.RIGHT: right = true;
                ((btRigidBody) player2.body).applyCentralImpulse(moveRight);
                break;
            default: return false;
        }*/
        return true;
    }

    @Override
    public boolean keyUp (int keycode) {
        super.keyUp(keycode);
        switch(keycode) {
            case Input.Keys.UP: up = false; break;
            case Input.Keys.DOWN: down = false; break;
            case Input.Keys.LEFT: left = false; break;
            case Input.Keys.RIGHT: right = false; break;
            default: return false;
        }
        Gdx.app.log("RAY pick", "RAY pick");
        return true;
    }

    public void updateImpulse(Vector3 newImpulseVector, int playerID)
    {
        Gdx.app.log("HEJ!", "Updating impulse for player: " + (playerID + 1));
        //playerList.get(playerID).setPosition
        playerList.get(playerID).setImpulseVector(newImpulseVector);
        playerEntityList.get(playerID).body.activate();
        ((btRigidBody)playerEntityList.get(playerID).body).applyCentralImpulse(newImpulseVector);
    }

    public void updatePositions(Vector<Vector3> checkCharPos, Vector<Vector3> checkCharRot)
    {
        if(playerCreated)
        {
            Matrix4 tmp;
            Quaternion tmpq;
            for (int ide = 1; ide <= playerEntityList.size(); ++ide)
            {
                tmpq = new Quaternion().setEulerAngles(checkCharRot.get(ide - 1).x, checkCharRot.get(ide - 1).y, checkCharRot.get(ide - 1).z);
                tmp = new Matrix4().set(tmpq);
                playerEntityList.get(ide - 1).body.activate();
                world.entities.get(ide).body.setWorldTransform(tmp.setTranslation(checkCharPos.get(ide - 1)));
                //((btRigidBody)world.entities.get(ide).body).setAngularVelocity(checkCharRot.get(ide - 1));
            }
        }
    }

    @Override
    public void render ()
    {

        if(app.createServerScreen.create != null)
        {
            Vector<Vector3> tempPosList = new Vector<Vector3>();
            Vector<Vector3> tempRotList = new Vector<Vector3>();
            for(int ide = 1; ide <= playerEntityList.size(); ++ide)
            {
                Vector3 tmpv = new Vector3(), tmpr;
                Quaternion tmpq = new Quaternion();
                world.entities.get(ide).body.getWorldTransform().getTranslation(tmpv);
                world.entities.get(ide).body.getWorldTransform().getRotation(tmpq);
                tmpr = new Vector3(tmpq.getYaw(), tmpq.getPitch(), tmpq.getRoll());
                tempPosList.add(tmpv);
                tempRotList.add(tmpr);
            }
            for(int idu = 0; idu < playerEntityList.size(); ++idu)
            {
                app.createServerScreen.create.sendCharData(tempPosList, tempRotList);
            }
        }
        //TODO if(app.joinServerScreen.join != null)
            //TODO if(!app.joinServerScreen.join.isAlive())
                //TODO Om servern kopplar ifrån/enheten kopplar från servern, boota till main menu.

        super.render();

        if(instance != null) {
            modelBatch.begin(camera);
            modelBatch.render(instance);
            modelBatch.end();

            controller.update(Gdx.graphics.getDeltaTime());
        }

        /*if (app.assets.update() && loading) {

            Model fotball = app.assets.get("3d/football2.g3dj", Model.class);
            String id = fotball.nodes.get(0).id;
            Model football = app.assets.get("3d/football2.g3dj", Model.class);
            String id = football.nodes.get(0).id;

        if (app.assets.update() && loading) {
            Model football = app.assets.get("3d/balls/football2.g3dj", Model.class);
            String id = football.nodes.get(0).id;

            Model apple = app.assets.get("3d/balls/apple.g3dj", Model.class);
            String id2 = apple.nodes.get(0).id;
            Node node = apple.getNode(id2);
            node.scale.set(0.8f, 0.8f, 0.8f);

            Model peach = app.assets.get("3d/balls/peach.g3dj", Model.class);
            String id3 = peach.nodes.get(0).id;
            Node node2 = peach.getNode(id3);

            player_1 = new Player(football, "football");
            world.addConstructor("test1", player_1.bulletConstructor);
            /*if(app.joinServerScreen.join != null)
            {
                world.addConstructor("test1", app.joinServerScreen.join.constructor);
                app.joinServerScreen.join.playerChar = world.add("test1", 0, 3.5f, 2.5f);
            }*/
           /* player1 = world.add("test1", 0, 3.5f, 2.5f);
            player1.body.setContactCallbackFlag(1);
            player1.body.setContactCallbackFilter(1);
            playerEntityList.add(player1);

            player_2 = new Player(apple, "apple");
            world.addConstructor("test2", player_2.bulletConstructor);
            player2 = world.add("test2", 0, 3.5f, 0.5f);
            player2.body.setContactCallbackFilter(1);

            player_3 = new Player(peach, "peach");
            world.addConstructor("test3", player_3.bulletConstructor);
            player3 = world.add("test3", 0, 3.5f, -2.5f);
            player3.body.setContactCallbackFilter(1);
            playerEntityList.add(player1);

            player_4 = new Player(peach, "peach");
            world.addConstructor("test4", player_4.bulletConstructor);
            player4 = world.add("test4", 0, 3.5f, -2.5f);
            player4.body.setContactCallbackFilter(1);

            Gdx.app.log("Loaded", "LOADED");
            loading = false;
            playerCreated = true;
*/
        // Count the score timer down.
        /*if(collisionHappened){
            scoreTimer -= 1f;
            if(scoreTimer < 0) { collisionHappened = false; }
            //Gdx.app.log("Score Timer = ", "" + scoreTimer);
        }*/

        // Points
          /*if(app.assets.update() && playerCreated) {
                  if ((((btRigidBody) playerEntityList.get(1).body).getCenterOfMassPosition().y < 0) && (((btRigidBody) playerEntityList.get(1).body).getCenterOfMassPosition().y > -0.08)
          if(app.assets.update() && playerCreated) {
              if ((((btRigidBody) player2.body).getCenterOfMassPosition().y < 0) && (((btRigidBody) player2.body).getCenterOfMassPosition().y > -0.08)
                          && (collisionUserId0 == 2 || collisionUserId1 == 2) && scoreTimer > 0) {
                  player_1.setScore(10);
                  Gdx.app.log("PLAYER2", "KRASH");

              }
              if((((btRigidBody) player3.body).getCenterOfMassPosition().y < 0) && (((btRigidBody) player3.body).getCenterOfMassPosition().y > -0.08)
                      && (collisionUserId0 == 3 ||  collisionUserId1 == 3) && scoreTimer > 0){
                  player_2.setScore(10);
                  Gdx.app.log("PLAYER3", "KRASH");
              }
            // Gameover
            if(((btRigidBody) playerEntityList.get(thisUnitId).body).getCenterOfMassPosition().y < 0 && !gameOverGameScreen ){
              if(((btRigidBody) player1.body).getCenterOfMassPosition().y < 0 && !gameOverGameScreen ){
                Gdx.app.log("Fall", "fall");
                player_2.setScore(20);
                player_3.setScore(20);

                // Add 1 to the current round
                int current_round = PropertiesSingleton.getInstance().getRound();
                PropertiesSingleton.getInstance().setRound(current_round);
                System.out.println("Round: " + current_round);
                gameOverGameScreen = true;
            }
            if(gameOverGameScreen)
                startGameOverTimer();
        }*/

        // Set the score
        /*for(int idu = 0; idu < PropertiesSingleton.getInstance().getNrPlayers(); ++idu)
        {
            //LabelScoreList..setText("Score " + playerList.get(idu).getId() + ": " + playerList.get(idu).getScore());
        }
        if(playerCreated) {
            LabelScorePlayer1.setText("Score player 1: " + player_1.getScore());
            LabelScorePlayer2.setText("Score player 2: " + player_2.getScore());
            LabelScorePlayer3.setText("Score player 3: " + player_3.getScore());
        }
              }
              if(gameOverGameScreen)
                  startGameOverTimer();
        }


        // Draw the sorted scores.
        drawScores();
        //draw countdown timer
        if(loading == false){
            countDown();
        }
        */


        if(DataHolder.getInstance().getIsTracking() == false)
        {
            labelTitle.setPosition(Gdx.graphics.getHeight() / 2 - labelTitle.getWidth() / 2, labelTitle.getHeight() * 2);
        }
        else
        {
            labelTitle.setPosition(Gdx.graphics.getHeight() / 2 - labelTitle.getWidth() / 2, -labelTitle.getHeight() * 2);
        }

        stage.draw();
        scoreStage.draw();
    }

    public static btConvexHullShape createConvexHullShape (final Model model, boolean optimize) {
        final Mesh mesh = model.meshes.get(0);
        final btConvexHullShape shape = new btConvexHullShape(mesh.getVerticesBuffer(), mesh.getNumVertices(), mesh.getVertexSize());
        if (!optimize) return shape;
        // now optimize the shape
        final btShapeHull hull = new btShapeHull(shape);
        hull.buildHull(shape.getMargin());
        final btConvexHullShape result = new btConvexHullShape(hull);
        // delete the temporary shape
        shape.dispose();
        hull.dispose();
        return result;
    }

    /*@Override
    public void update () {
        float delta = Gdx.graphics.getRawDeltaTime();
        time += delta;
        super.update();
        if (contactCache != null) contactCache.update(delta);
    }*/

    @Override
    public void show() {


    }

    @Override
    public void render(float delta) {
        render();
    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        super.dispose();
        //stage.dispose();
        if (rayTestCB != null) {rayTestCB.dispose(); rayTestCB = null;}
        //scoreStage.dispose(); // Borde disposas men det blir hack till nästa screen
    }

    // Sorts and draws the scores.
    private void drawScores(){
        // TODO: Borde egentligen inte kallas varenda renderingsframe, borde enbart köras när det sker förändringar i någons score. Därför ska den kallas i poängsystemet i render(), men vi har ju inget riktigt poängsystem än.
        if(playerCreated) {
            Collections.sort(playerList);

            LabelScorePlayer1.setText("Score " + playerList.get(0).getModelName() + ": " + playerList.get(0).getScore());
            LabelScorePlayer2.setText("Score " + playerList.get(1).getModelName() + ": " + playerList.get(1).getScore());
            LabelScorePlayer3.setText("Score " + playerList.get(2).getModelName() + ": " + playerList.get(2).getScore());
            LabelScorePlayer4.setText("Score " + playerList.get(3).getModelName() + ": " + playerList.get(3).getScore());

            // TODO: KOD NEDAN ÄR INTE FÄRDIG OCH DEN ÄR TILL FÖR KUNNA ANIMERA NÄR NÅGON AVANCERAR I PLACERING FÖR SCORE.
            /*
            // Set the score for the players in the same label.
            LabelScorePlayer1.setText("P1: " + playerList.get(0).getScore());
            LabelScorePlayer2.setText("P2: " + playerList.get(1).getScore());
            LabelScorePlayer3.setText("P3: " + playerList.get(2).getScore());
            LabelScorePlayer4.setText("P4: " + playerList.get(3).getScore());

            // Take the actors for the score labels and move them to advance in positions. TODO: ta bort getHeight funktionsanropet.
            stage.getRoot().getChildren().get(0).setPosition(20, Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 20 * 1));
            stage.getRoot().getChildren().get(1).setPosition(20, Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 20 * 2));
            stage.getRoot().getChildren().get(2).setPosition(20, Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 20 * 3));
            stage.getRoot().getChildren().get(3).setPosition(20, Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 20 * scoreLabelAnimationTimer));
            */
        }
    }
/*
    private void startGameOverTimer() {

        scoreStage.act();

        gameOverTimer += Gdx.graphics.getDeltaTime();

        if (gameOverTimer > 0.5) {
            super.setGameOver();
            scoreStage.getRoot().addAction(Actions.sequence(Actions.delay(1.2f), Actions.moveTo(0, 0, 0.5f), Actions.delay(1),
                    Actions.run(new Runnable() {
                        public void run() {

                            PropertiesSingleton.getInstance().setNrPlayers(n_players);

                            // Prepare necessary data for the highscore screen.

                            // Set the scores.
                            PropertiesSingleton.getInstance().setPlayer1Score(player_1.getScore());
                            PropertiesSingleton.getInstance().setPlayer2Score(player_2.getScore());
                            PropertiesSingleton.getInstance().setPlayer3Score(player_3.getScore());
                            PropertiesSingleton.getInstance().setPlayer4Score(player_4.getScore());

                            // Get the model names.
                            PropertiesSingleton.getInstance().setPlayer1Ball(player_1.getModelName());
                            PropertiesSingleton.getInstance().setPlayer2Ball(player_2.getModelName());
                            PropertiesSingleton.getInstance().setPlayer3Ball(player_3.getModelName());
                            PropertiesSingleton.getInstance().setPlayer4Ball(player_4.getModelName());

                            // Prepare the "Ball String" so that it can later be sent over the network as a string.
                            PropertiesSingleton.getInstance().createBallString();

                            app.setScreen(new ScoreScreen(app));
                            dispose();
                        }
                    })));
            }
        }
*/

    //--------------Countdown-------------------------------------
    private void countDown() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        totalTime -= deltaTime;
        float seconds = totalTime - deltaTime;

        if(totalTime < 1) {
            LabelCountdown.setText("GO");
            if(totalTime <0){
                LabelCountdown.setVisible(false);
                countdownFinished = true;
            }
        }else{
            LabelCountdown.setText(String.format("%.0f", seconds));
        }

    }




    }