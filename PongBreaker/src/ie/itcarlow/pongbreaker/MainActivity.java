package ie.itcarlow.pongbreaker;

import java.util.Random;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.ui.activity.BaseGameActivity;

import android.view.MotionEvent;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;


public class MainActivity extends BaseGameActivity implements IUpdateHandler {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 480;
	private static final int CAMERA_HEIGHT = 720;

	// ===========================================================
	// Fields
	// ===========================================================

	private BitmapTextureAtlas mTextureBall;
	private ITextureRegion mBallTextureRegion;
	private Scene mScene;
	private Sprite mBall;
	
	private PhysicsWorld mPhysicsWorld;
	private FixtureDef BALL_FIX; //the scale that box2d uses to convert between real position and pixels.
	private FixtureDef BOUND_FIX;
	
	private Vector2 vel;
	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public EngineOptions onCreateEngineOptions() {
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		return new EngineOptions(true, ScreenOrientation.PORTRAIT_SENSOR, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
	}

    @Override
	public void onCreateResources(
       OnCreateResourcesCallback pOnCreateResourcesCallback)
			throws Exception {
    	
    	 BALL_FIX = PhysicsFactory.createFixtureDef(1f,1f,0f);
    	 BOUND_FIX = PhysicsFactory.createFixtureDef(0f, 0f, 0f);

    	 loadGfx();
		 pOnCreateResourcesCallback.onCreateResourcesFinished();

    }

    private void loadGfx() {

        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");  
        mTextureBall = new BitmapTextureAtlas(getTextureManager(), 90, 90);  
        mBallTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mTextureBall, this, "ball.png", 0, 0);
        mTextureBall.load();
    }

    @Override
  	public void onCreateScene(OnCreateSceneCallback pOnCreateSceneCallback)
  			throws Exception {
    	
    	
    	this.mScene = new Scene();
  		this.mScene.setBackground(new Background(0, 125, 58));
  		setUpBox2DWorld();
  		mPhysicsWorld.setContactListener(CreateContactListener());
  	    pOnCreateSceneCallback.onCreateSceneFinished(this.mScene);  		
  	}


    @Override
	public void onPopulateScene(Scene pScene, OnPopulateSceneCallback pOnPopulateSceneCallback) 
          throws Exception {

    	
    	mEngine.registerUpdateHandler(this);
       // Setup coordinates for the sprite in order that it will
       //  be centered on the camera.
	   final float centerX = (CAMERA_WIDTH - this.mBallTextureRegion.getWidth()) / 2;
	   final float centerY = (CAMERA_HEIGHT - this.mBallTextureRegion.getHeight()) / 2;
	   
	   //FINAL MEANS MAGIC
	   final Random random = new Random(); 
	   // Create the ball and add it to the scene.
	   
	   mBall = new Sprite(centerX, centerY, this.mBallTextureRegion, this.getVertexBufferObjectManager())
	   {
           @Override
           public boolean onAreaTouched(final TouchEvent event,
                                        final float pTouchAreaLocalX,
                                        final float pTouchAreaLocalY) {
        	   if (event.getAction() == MotionEvent.ACTION_UP) {
        		   //setBodyPosition(this, event.getX() - this.getWidth() / 2, event.getY() - this.getHeight() / 2);
        		   float speed = 0.1f;
        		   vel = new Vector2(random.nextInt(100) / speed, random.nextInt(100) / speed);
        		   Vector2 velocity = Vector2Pool.obtain(vel.x, vel.y);
        		   Body b = (Body) this.getUserData();
        		   b.applyLinearImpulse(velocity, b.getWorldCenter());
        		   Vector2Pool.recycle(velocity);        		   
        	   }
               return true;
           }
       };
	   
	   this.mScene.registerTouchArea(mBall);
	   
	   createBall(mBall, "ball1");  
	   mScene.attachChild(mBall);
	   
	   createBound(CAMERA_WIDTH / 2, 0, CAMERA_WIDTH, 0, "bound1"); //top
	   createBound(CAMERA_WIDTH / 2, CAMERA_HEIGHT, CAMERA_WIDTH, 0, "bound2"); //bottom
	   createBound(0, CAMERA_HEIGHT / 2, 0, CAMERA_HEIGHT, "bound3");
	   createBound(CAMERA_WIDTH, CAMERA_HEIGHT / 2, 0, CAMERA_HEIGHT, "bound4");
	   
	   pOnPopulateSceneCallback.onPopulateSceneFinished();
    }

	// ===========================================================
	// Methods
	// ===========================================================

    private void setUpBox2DWorld() {
    	// Set up your physics world here.
    	final Vector2 v = Vector2Pool.obtain(0,0);
    	mPhysicsWorld = new PhysicsWorld(v, false);
    	Vector2Pool.recycle(v);
    	this.mScene.registerUpdateHandler(mPhysicsWorld);  		
    }
    
    private void createBall(final Sprite sprite, String name) {
    	// Create your Box2D bodies here.
    	Body body = PhysicsFactory.createCircleBody(mPhysicsWorld, sprite, BodyType.DynamicBody, BALL_FIX);
    	body.setFixedRotation(true);
    	body.setLinearDamping(0f);
    	body.setUserData(name);
    	sprite.setUserData(body);
    	mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(sprite, body, true, true));    	
    }
    
    private void createBound(float x1, float y1, float width, float height, String name) {
    	// Create your Box2D bodies here.
 
    	Body body = PhysicsFactory.createBoxBody(mPhysicsWorld, x1, y1, width, height, BodyType.StaticBody, BOUND_FIX);
    	body.setLinearDamping(0f);
    	body.setUserData(name);
    	mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(mBall, body, true, true));    	
    }
    
    /*
     * Helper method that translates the associated physics body to the specified coordinates.
     * 
	 * @param pX The desired x coordinate for this sprite.
	 * @param pY The desired y coordinate for this sprite.
     */
    private void setBodyPosition(final Sprite sprite, final float pX, final float pY) {
    	
    	final Body body = (Body) sprite.getUserData();
        final float widthD2 = sprite.getWidth() / 2;
        final float heightD2 = sprite.getHeight() / 2;
        final float angle = body.getAngle(); // keeps the body angle       
        final Vector2 v2 = Vector2Pool.obtain((pX + widthD2) / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT, (pY + heightD2) / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT);
        body.setTransform(v2, angle);
        Vector2Pool.recycle(v2);
    }

	@Override
	public void onUpdate(float pSecondsElapsed) {
		// TODO Auto-generated method stub	
	
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
	
	private ContactListener CreateContactListener() {
		ContactListener myContactListener = new ContactListener() {
			public void beginContact(Contact contact) {
				String a = (String) contact.getFixtureA().getBody().getUserData();
				String b = (String) contact.getFixtureB().getBody().getUserData();
				
				if (a != null && b != null) {
				}
			}

			@Override
			public void endContact(Contact contact) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void preSolve(Contact contact, Manifold oldManifold) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void postSolve(Contact contact, ContactImpulse impulse) {
				// TODO Auto-generated method stub
				
			}
		};
		return myContactListener;
	}
    

	    
    // ===========================================================
 	// Inner and Anonymous Classes
 	// ===========================================================
    
}
