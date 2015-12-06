/* Copyright 2010 Kenneth 'Impaler' Ferland

 This file is part of Khazad.

 Khazad is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Khazad is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Khazad.  If not, see <http://www.gnu.org/licenses/> */

package Interface;

import Core.Main;
import Game.Actor;
import Game.Citizen;
import Game.Game;
import Map.CubeCoordinate;
import Renderer.TerrainRenderer;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

import com.jme3.material.Material;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Plane;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;

import com.jme3.scene.Spatial;
import com.jme3.scene.Node;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;

import com.jme3.bounding.BoundingBox;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;

import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;

import Renderer.MapRenderer;

/**
 * Manages the main games parrelel projection Camera
 *
 * @author Impaler
 */
public class GameCameraState extends AbstractAppState implements ActionListener, AnalogListener {

	public enum CameraMode {

		NORMAL,
		SELECT_VOLUME,
		SELECTING_VOLUME,
		SELECT_SURFACE,
		SELECTING_SURFACE
	}

	private Node rootnode;
	private Node Mapnode;
	private Node LookNode;
	private SimpleApplication app;
	private GameCamera MainCamera;
	private AppStateManager state;
	String[] InputStrings;
	private CameraMode CurrentMode = CameraMode.NORMAL;
	// Key state tracking Booleans
	private boolean LeftDown;
	private boolean RightDown;
	private boolean MiddleDown;
	private boolean LShiftDown;
	private boolean RShiftDown;
	private boolean Shift;
	private boolean RightwardPaning;
	private boolean LeftwardPaning;
	private boolean UpwardPaning;
	private boolean DownwardPaning;
	private int PanningSpeed = 3;
	private float OldMouseX;
	private float OldMouseY;
	private float XChange;
	private float YChange;
	private Plane SelectionPlane = null;
	private CubeCoordinate MouseLocation = new CubeCoordinate();
	public CubeCoordinate SelectionOrigin = new CubeCoordinate();
	public CubeCoordinate SelectionTerminus = new CubeCoordinate();
	public VolumeSelection Volume;
	protected int SliceTop;
	protected int SliceBottom;
	protected int ViewLevels;
	protected int ViewMax, ViewMin;
	private boolean mouseWheelEnabled = true;
	private Actor selectedActor;

	public GameCameraState() {
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		this.app = (SimpleApplication) app;
		this.state = stateManager;
		this.rootnode = this.app.getRootNode();

		MapRenderer rend = stateManager.getState(MapRenderer.class);
		this.Mapnode = rend.getMapNode();

		if (app.getInputManager() != null) {

			if (MainCamera == null) {
				LookNode = new Node("LookNode");
				this.app.getRootNode().attachChild(LookNode);
				LookNode.setLocalTranslation(1, 2, 3);

				Sphere eye = new Sphere(10, 10, 0.5f);
				Geometry EyeBall = new Geometry("EyeBall", eye);
				EyeBall.setLocalTranslation(new Vector3f(0, 0, 0));
				Material mat1 = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
				mat1.setColor("Color", ColorRGBA.White);
				EyeBall.setMaterial(mat1);
				LookNode.attachChild(EyeBall);
				LookNode.setCullHint(Spatial.CullHint.Always);

				MainCamera = new GameCamera(app.getCamera(), LookNode);
			}
		}
		registerWithInput(app.getInputManager());
	}

	protected void convertMouseMovementToVector() {
		Vector2f Mouse = app.getInputManager().getCursorPosition();

		XChange = OldMouseX - Mouse.x;
		YChange = OldMouseY - Mouse.y;

		OldMouseX = Mouse.x;
		OldMouseY = Mouse.y;
	}

	protected Vector3f createTranslationVector(float X, float Y) {
		Vector3f LookVector = MainCamera.TargetNode.getWorldTranslation().subtract(MainCamera.CamNode.getWorldTranslation());

		LookVector.normalizeLocal();
		float zComp = LookVector.z;
		LookVector.z = 0;
		LookVector.normalizeLocal();

		Vector3f TempUpVector = Vector3f.UNIT_Z;

		Vector3f CrossProduct = TempUpVector.cross(LookVector);
		CrossProduct.normalizeLocal();

		Vector3f Temp1 = CrossProduct.mult(-X).mult(MainCamera.TranslationFactor);  //
		Vector3f Temp2 = LookVector.mult(-Y).mult(MainCamera.TranslationFactor).divide(zComp);

		return Temp1.add(Temp2);
	}

	public void onAction(String name, boolean keyPressed, float tpf) {
		if (this.isEnabled()) {
			if (name.equals("LeftClick")) {
				LeftDown = keyPressed;

				if (CurrentMode == CameraMode.SELECTING_VOLUME && !keyPressed)
					completeVolumeSelection();

				if (CurrentMode == CameraMode.SELECT_VOLUME && keyPressed)
					setMode(CameraMode.SELECTING_VOLUME);

				if (selectedActor != null && keyPressed) {
					if (selectedActor instanceof Citizen) {
						Citizen citizen = (Citizen) selectedActor;
						// open a window
						Game game = Main.app.getStateManager().getState(Game.class);
						game.getGameScreenController().spawnCitizenWindow(citizen);
					}
				}
			}

			if (name.equals("RightClick")) {
				RightDown = keyPressed;

				if (CurrentMode == CameraMode.SELECT_VOLUME) {
					setMode(CameraMode.NORMAL);
				}
				if (CurrentMode == CameraMode.SELECTING_VOLUME) {
					setMode(CameraMode.NORMAL);
				}
			}

			if (name.equals("MiddleClick")) {
				MiddleDown = keyPressed;
				if (MiddleDown) {
					LookNode.setCullHint(Spatial.CullHint.Dynamic);
				} else {
					LookNode.setCullHint(Spatial.CullHint.Always);
				}
			}

			if (name.equals("ArrowUp") && keyPressed) {
				changeViewLevel(1);
			}

			if (name.equals("ArrowDown") && keyPressed) {
				changeViewLevel(-1);
			}

			if (name.equals("PanRight"))
				RightwardPaning = keyPressed;

			if (name.equals("PanLeft"))
				LeftwardPaning = keyPressed;

			if (name.equals("PanUp"))
				UpwardPaning = keyPressed;

			if (name.equals("PanDown"))
				DownwardPaning = keyPressed;

			if (name.equals("RShift") && keyPressed) {
				RShiftDown = keyPressed;
				Shift = RShiftDown || LShiftDown;
			}

			if (name.equals("LShift") && keyPressed) {
				LShiftDown = keyPressed;
				Shift = RShiftDown || LShiftDown;
			}

		}
	}

	private void analogNormal(String name, float value, float tpf) {
		updateMousePosition();

		if (name.equals("mouseLeft")) {
			if (MiddleDown) {
				MainCamera.rotateCamera(value);
			} else {
				if (RightDown) {
					MainCamera.translateCamera(createTranslationVector(XChange, YChange));
					XChange = YChange = 0; // Consume the Mouse movement
				}
			}
		} else if (name.equals("mouseRight")) {
			if (MiddleDown) {
				MainCamera.rotateCamera(-value);
			} else {
				if (RightDown) {
					MainCamera.translateCamera(createTranslationVector(XChange, YChange));
					XChange = YChange = 0; // Consume the Mouse movement
				}
			}
		} else if (name.equals("mouseUp")) {
			if (MiddleDown) {
				MainCamera.pitchCamera(value);
			} else {
				if (RightDown) {
					MainCamera.translateCamera(createTranslationVector(XChange, YChange));
					XChange = YChange = 0; // Consume the Mouse movement
				}
			}
		} else if (name.equals("mouseDown")) {
			if (MiddleDown) {
				MainCamera.pitchCamera(-value);
			} else {
				if (RightDown) {
					MainCamera.translateCamera(createTranslationVector(XChange, YChange));
					XChange = YChange = 0; // Consume the Mouse movement
				}
			}
		} else if (name.equals("ZoomIn")) {
			if (MiddleDown) {
				changeViewLevel(-1);
			} else if (mouseWheelEnabled) {
				MainCamera.zoomCamera(value);
				TerrainRenderer Terrain = state.getState(TerrainRenderer.class);
				Terrain.setLevelofDetail(MainCamera.zoomFactor);
			}
		} else if (name.equals("ZoomOut")) {
			if (MiddleDown) {
				changeViewLevel(1);
			} else if (mouseWheelEnabled) {
				MainCamera.zoomCamera(-value);
				TerrainRenderer Terrain = state.getState(TerrainRenderer.class);
				Terrain.setLevelofDetail(MainCamera.zoomFactor);
			}
		}
	}

	private void analogSelectingVolume(String name, float value, float tpf) {

		if (Shift) { // Z axis stretching
			Vector3f LookVector = MainCamera.TargetNode.getWorldTranslation().subtract(MainCamera.CamNode.getWorldTranslation());

			LookVector.normalizeLocal();
			float zComp = LookVector.z;
			LookVector.z = 0;
			LookVector.normalizeLocal();

		} else { // XY plane intersection
			Ray ray = MainCamera.getMouseRay(app.getInputManager().getCursorPosition());
			Vector3f IntersectLocation = new Vector3f();
			ray.intersectsWherePlane(SelectionPlane, IntersectLocation);

			SelectionTerminus.set((int) IntersectLocation.x, (int) IntersectLocation.y, (int) SelectionOrigin.Z);
			Volume.setSize(SelectionOrigin, SelectionTerminus);
		}

		if (name.equals("mouseLeft")) {
			if (MiddleDown) {
				MainCamera.rotateCamera(value);
			} else {
				if (RightDown) {
					MainCamera.translateCamera(createTranslationVector(XChange, YChange));
					XChange = YChange = 0; // Consume the Mouse movement		
				}
			}
		} else if (name.equals("mouseRight")) {
			if (MiddleDown) {
				MainCamera.rotateCamera(-value);
			} else {
				if (RightDown) {
					MainCamera.translateCamera(createTranslationVector(XChange, YChange));
					XChange = YChange = 0; // Consume the Mouse movement
				}
			}
		} else if (name.equals("mouseUp")) {
			if (MiddleDown) {
				MainCamera.pitchCamera(value);
			} else {
				if (RightDown) {
					MainCamera.translateCamera(createTranslationVector(XChange, YChange));
					XChange = YChange = 0; // Consume the Mouse movement
				}
			}
		} else if (name.equals("mouseDown")) {
			if (MiddleDown) {
				MainCamera.pitchCamera(-value);
			} else {
				if (RightDown) {
					MainCamera.translateCamera(createTranslationVector(XChange, YChange));
					XChange = YChange = 0; // Consume the Mouse movement
				}
			}
		} else if (name.equals("ZoomIn")) {
			MainCamera.zoomCamera(value);
			TerrainRenderer Terrain = state.getState(TerrainRenderer.class);
			Terrain.setLevelofDetail(MainCamera.zoomFactor);
		} else if (name.equals("ZoomOut")) {
			MainCamera.zoomCamera(-value);
			TerrainRenderer Terrain = state.getState(TerrainRenderer.class);
			Terrain.setLevelofDetail(MainCamera.zoomFactor);
		}

	}

	public void onAnalog(String name, float value, float tpf) {
		convertMouseMovementToVector();
		switch (CurrentMode) {
			case NORMAL:
				analogNormal(name, value, tpf);
				break;

			case SELECT_VOLUME:
				analogNormal(name, value, tpf);
				break;

			case SELECTING_VOLUME:
				analogSelectingVolume(name, value, tpf);
				break;
		}
	}

	public void registerWithInput(InputManager inputManager) {
		String[] inputs = {"LeftClick", "RightClick", "MiddleClick", "mouseDown", "mouseUp", "mouseLeft", "mouseRight", "ZoomIn", "ZoomOut", "ArrowUp", "ArrowDown", "RShift", "LShift", "PanUp", "PanDown", "PanRight", "PanLeft"};
		this.InputStrings = inputs;



		inputManager.addListener(this, InputStrings);
	}

	public void unregisterInput(InputManager inputManager) {
		inputManager.removeListener(this);
	}

	public void updateMousePosition() {
		MapRenderer rend = this.app.getStateManager().getState(MapRenderer.class);
		this.Mapnode = rend.getMapNode();

		Ray ray = MainCamera.getMouseRay(app.getInputManager().getCursorPosition());
		Vector3f IntersectLocation = new Vector3f();
		selectedActor = null;

		if (Mapnode != null) {
			CollisionResults results = new CollisionResults();
			Mapnode.collideWith(ray, results);

			if (results.size() > 0) {
				// The closest collision point is what was truly hit:
				CollisionResult closest = results.getClosestCollision();
				identifyNode(closest.getGeometry().getParent());

				Vector3f contact = closest.getContactPoint();
				Vector3f normal = closest.getContactNormal();
				IntersectLocation = contact.subtract(normal.mult(.001f));

			} else {
				ray.intersectsWherePlane(new Plane(Vector3f.UNIT_Z, getSliceTop()), IntersectLocation);
			}
			int x = Math.round(IntersectLocation.getX());
			int y = Math.round(IntersectLocation.getY());
			int z = Math.round(IntersectLocation.getZ());
			MouseLocation.set(x, y, z);
		}
	}

	private void identifyNode(Node node) {
		if (node == null) {
			return;
		}
		String nodeName = node.getName();
		if (nodeName.startsWith("ActorNode-")) {
			// an actor
			// TODO highlight the actor in some way to show that the mouse is over it
			int actorId = Integer.parseInt(nodeName.substring(10));
			Game game = app.getStateManager().getState(Game.class);
			selectedActor = game.getActors().get(actorId);
			return;
		}
		//System.out.println("Skipped: " + nodeName);
		// keep searching;
		identifyNode(node.getParent());
	}

	public CubeCoordinate getMouseLocation() {
		return MouseLocation;
	}

	public void setMode(CameraMode newMode) {
		if (CurrentMode != newMode) {
			CurrentMode = newMode;

			if (CurrentMode == CameraMode.SELECTING_VOLUME) {
				SelectionOrigin.copy(MouseLocation);
				SelectionTerminus.copy(MouseLocation);
				Volume = new VolumeSelection(SelectionTerminus, SelectionOrigin);

				SelectionPlane = new Plane(Vector3f.UNIT_Z, MouseLocation.Z);
				//this.rootnode.detachChild(SelectionBox);	
			}

			if (CurrentMode == CameraMode.NORMAL) {
				SelectionPlane = null;
			}
		}
	}

	public void completeVolumeSelection() {
		Game game = state.getState(Game.class);
		game.volumeSelectionCompleted(Volume);
		Volume = null;

		setMode(CameraMode.SELECT_VOLUME);
	}

	public void setViewSize(int max, int min) {
		ViewMax = max;
		ViewMin = min;
	}

	public void changeViewLevel(int Change) {
		if (Change != 0) {
			//if(SliceTop + Change > ViewMax) {
			//	Change = SliceTop - ViewMax;
			//}
			//if(SliceBottom + Change < ViewMin) {
			//	Change = SliceBottom - ViewMin;
			//}
			SliceTop += Change;
			SliceBottom += Change;
			ViewLevels = SliceTop - SliceBottom;

			//TargetNode.move(0, 0, Change);
			//CamNode.move(0, 0, Change);		
		}
	}

	public void setSlice(int newTop, int newBottome) {
		SliceTop = newTop;
		if (SliceBottom >= SliceTop)
			SliceBottom = SliceTop - 1;

		SliceBottom = newBottome;
		if (SliceTop <= SliceBottom)
			SliceTop = SliceBottom + 1;

		ViewLevels = SliceTop - SliceBottom;
		
		if (this.state != null) {
			MapRenderer render = state.getState(MapRenderer.class);
			if (render != null) {
				render.setSliceLevels(SliceTop, SliceBottom);
			}
		}
	}

	public void setSliceTop(int newValue) {
		//TargetNode.move(0, 0, newValue - SliceTop);
		//CamNode.move(0, 0, newValue - SliceTop);

		SliceTop = newValue;
		if (SliceBottom >= SliceTop)
			SliceBottom = SliceTop - 1;

		ViewLevels = SliceTop - SliceBottom;
		
		if (this.state != null) {
			MapRenderer render = state.getState(MapRenderer.class);
			if (render != null) {
				render.setSliceLevels(SliceTop, SliceBottom);
			}
		}
	}

	public void setSliceBottom(int newValue) {
		//TargetNode.move(0, 0, newValue - SliceBottom);
		//CamNode.move(0, 0, newValue - SliceBottom);

		SliceBottom = newValue;
		if (SliceTop <= SliceBottom)
			SliceTop = SliceBottom + 1;

		ViewLevels = SliceTop - SliceBottom;
		
		if (this.state != null) {
			MapRenderer render = state.getState(MapRenderer.class);
			if (render != null) {
				render.setSliceLevels(SliceTop, SliceBottom);
			}
		}
	}

	public int getSliceTop() {
		return SliceTop;
	}

	public int getSliceBottom() {
		return SliceBottom;
	}

	public float getZoom() {
		return this.MainCamera.zoomFactor;
	}

	@Override
	public void update(float tpf) {
		if (RightwardPaning)
			MainCamera.translateCamera(createTranslationVector(PanningSpeed, 0));

		if (LeftwardPaning)
			MainCamera.translateCamera(createTranslationVector(-PanningSpeed, 0));

		if (UpwardPaning)
			MainCamera.translateCamera(createTranslationVector(0, PanningSpeed));

		if (DownwardPaning)
			MainCamera.translateCamera(createTranslationVector(0, -PanningSpeed));
	}

	@Override
	public void cleanup() {
		super.cleanup();

		unregisterInput(app.getInputManager());
	}

	public boolean isMouseWheelEnabled() {
		return mouseWheelEnabled;
	}

	public void setMouseWheelEnabled(boolean mouseWheelEnabled) {
		this.mouseWheelEnabled = mouseWheelEnabled;
	}

	public Actor getSelectedActor() {
		return selectedActor;
	}

	public void pointCameraAt(CubeCoordinate mapCoordinate) {
		// change to the same Z level as the target
		SliceTop = mapCoordinate.Z;
		SliceBottom = SliceTop + ViewLevels;
		// point camera at the target
		Vector3f target = new Vector3f(mapCoordinate.X, mapCoordinate.Y, mapCoordinate.Z);
		MainCamera.pointCameraAt(target);
	}
	
	public boolean contains(BoundingBox box) {
		return MainCamera.contains(box);
	}
}
