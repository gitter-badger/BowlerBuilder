package com.neuronrobotics.bowlerbuilder.controller.robotmanager;

import com.google.common.base.Throwables;
import com.neuronrobotics.bowlerbuilder.LoggerUtilities;
import com.neuronrobotics.bowlerbuilder.controller.cadengine.CadEngine;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseCadManager;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.MobileBase;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.DeviceManager;
import com.neuronrobotics.sdk.util.ThreadUtil;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.ProgressIndicator;
import org.apache.commons.io.IOUtils;
import org.greenrobot.eventbus.EventBus;

public class RobotManager {

  private static final Logger logger
      = LoggerUtilities.getLogger(RobotManager.class.getSimpleName());

  public RobotManager(CadEngine cadEngine, ProgressIndicator progressIndicator) {
    try {
      String[] file = {"https://github.com/madhephaestus/SeriesElasticActuator.git",
          "seaArm.xml"};
      String xmlContent = ScriptingEngine.codeFromGit(file[0], file[1])[0];
      MobileBase mobileBase = new MobileBase(IOUtils.toInputStream(xmlContent, "UTF-8"));
      mobileBase.setGitSelfSource(file);
      mobileBase.connect();
      MobileBaseCadManager mobileBaseCadManager = new MobileBaseCadManager(
          mobileBase, new BowlerMobileBaseUI(cadEngine));
      mobileBase.updatePositions();
      DeviceManager.addConnection(mobileBase, mobileBase.getScriptingName());
      EventBus.getDefault().post(new RobotLoadedEvent(mobileBase));
      mobileBaseCadManager.generateCad();
      logger.log(Level.INFO, "Waiting for cad to generate.");
      progressIndicator.progressProperty()
          .bind(MobileBaseCadManager.get(mobileBase).getProcesIndictor());
      ThreadUtil.wait(1000);
      while (MobileBaseCadManager.get(mobileBase).getProcesIndictor().get() < 1) {
        ThreadUtil.wait(1000);
      }
      while (true) {
        DHParameterKinematics leg0 = mobileBase.getAllDHChains().get(0);
        double zLift = 25;
        TransformNR current = leg0.getCurrentPoseTarget();
        current.translateZ(zLift);
        leg0.setDesiredTaskSpaceTransform(current, 2.0);
        ThreadUtil.wait(2000);

        current.translateZ(-zLift);
        leg0.setDesiredTaskSpaceTransform(current, 2.0);
        ThreadUtil.wait(2000);
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE,
          "Could not load assets for robot.\n" + Throwables.getStackTraceAsString(e));
    } catch (Exception e) {
      logger.log(Level.SEVERE,
          "Could not start building robot.\n" + Throwables.getStackTraceAsString(e));
    }
  }

}
