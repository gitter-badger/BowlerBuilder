package com.neuronrobotics.bowlerbuilder.controller.cadengine.view.element;

import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.Vertex;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Affine;

public class Line3D extends Cylinder {

  private double endZ;
  private double startZ;

  public Line3D(Vertex start, Vertex end) {
    this(start.pos, end.pos);
  }

  public Line3D(double[] start, double[] end) { //NOPMD
    this(start[0], start[1], start[2], end[0], end[1], end[2]);
  }

  public Line3D(Vector3d start, Vector3d end) {
    this(start.x, start.y, start.z, end.x, end.y, end.z);
  }

  public Line3D(double endX, double endY, double endZ) {
    this(0, 0, 0, endX, endY, endZ);
  }

  public Line3D(double startX, double startY, double startZ,
                double endX, double endY, double endZ) {
    super(
        0.1,
        Math.sqrt(Math.pow(endX - startX, 2)
            + Math.pow(endY - startY, 2)
            + Math.pow(endZ - startZ, 2))
    );

    double xDiff = endX - startX;
    double yDiff = endY - startY;
    double zDiff = endZ - startZ;
    double lineLen = getHeight();

    double xyProjection = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));

    Affine xy = new Affine();
    double rotY = Math.toDegrees(Math.atan2(xyProjection, zDiff));
    xy.appendRotation(-90 - rotY, 0, 0, 0, 0, 1, 0);

    Affine orent = new Affine();
    orent.appendRotation(90, 0, 0, 0, 0, 0, 1);
    orent.setTx(lineLen / 2);

    Affine zp = new Affine();
    double rotZ = Math.toDegrees(Math.atan2(xDiff, yDiff));
    zp.appendRotation(-90 - rotZ, 0, 0, 0, 0, 0, 1);
    Affine zTrans = new Affine();
    zTrans.setTx(startX);
    zTrans.setTy(startY);
    zTrans.setTz(startZ);

    getTransforms().add(zTrans);
    getTransforms().add(zp);
    getTransforms().add(xy);

    getTransforms().add(orent);

    Affine orent2 = new Affine();
    getTransforms().add(orent2);
  }

  public double getEndZ() {
    return endZ;
  }

  public void setEndZ(double endZ) {
    this.endZ = endZ;
  }

  public double getStartZ() {
    return startZ;
  }

  public void setStartZ(double startZ) {
    this.startZ = startZ;
  }

  public void setStrokeWidth(double radius) {
    setRadius(radius / 2);
  }

  public void setStroke(Color color) {
    Platform.runLater(() -> setMaterial(new PhongMaterial(color)));
  }

}
