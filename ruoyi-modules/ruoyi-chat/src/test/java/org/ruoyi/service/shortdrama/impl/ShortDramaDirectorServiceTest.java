package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaDirectorServiceTest {
    @Test void gateAndCannonFirePersistAndEventsCannotEscapeTheirTimeline() throws Exception {
        var doc=document();var gate=((ObjectNode)doc.path("objects").get(0)).deepCopy();gate.put("id","gate");gate.put("kind","castle");
        var cannon=gate.deepCopy();cannon.put("id","cannon");cannon.put("kind","cannon");cannon.put("fireFrame",168);
        ((com.fasterxml.jackson.databind.node.ArrayNode)doc.path("objects")).add(gate).add(cannon);
        var service=new ShortDramaDirectorService(mock(ShortDramaSoundService.class),root);
        service.save(1L,"gate-demo",2L,new ShortDramaDirectorService.Director(0,doc));
        assertEquals(doc,service.read(1L,"gate-demo",2L).document());
        cannon.put("fireFrame",192);assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(doc));
        cannon.put("fireFrame",168);gate.put("fireFrame",1);assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(doc));
    }
    @Test void extendedCaptureJointsAndLongerTimelineRoundTripAndInvalidMetadataIsRejected() throws Exception {
        var doc=document();doc.put("duration",60);var actor=(ObjectNode)doc.path("objects").get(0);
        actor.put("grounded",true);actor.put("quaternionPose",true);
        ((ObjectNode)actor.path("keys").get(0)).set("pose",json.readTree("{\"pelvis\":[0,20,0],\"leftShoulder\":[0,0,-12],\"leftHand\":[30,0,5],\"rightFoot\":[15,0,0]}"));
        doc.set("markers",json.readTree("[{\"frame\":1100,\"label\":\"交叉步\"}]"));
        var service=new ShortDramaDirectorService(mock(ShortDramaSoundService.class),root);
        service.save(1L,"capture",2L,new ShortDramaDirectorService.Director(0,doc));
        assertEquals(doc,service.read(1L,"capture",2L).document());
        actor.put("grounded","yes");assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(doc));
        actor.put("grounded",true);doc.put("duration",61);assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(doc));
        doc.put("duration",60);((ObjectNode)doc.path("markers").get(0)).put("frame",1440);assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(doc));
    }
    @TempDir Path root;
    private final ObjectMapper json = new ObjectMapper();
    private ObjectNode document() throws Exception { return (ObjectNode) json.readTree("""
      {"schema":1,"fps":24,"duration":8,"aspect":"16:9","objects":[
        {"id":"actor-1","kind":"actor","name":"人物","color":"#eeeeee","scale":[1,1,1],"visible":true,"locked":false,
         "keys":[{"frame":0,"position":[0,0,0],"yaw":0,"action":"walk"},{"frame":191,"position":[3,0,0],"yaw":90,"action":"idle"}]}],
       "camera":{"keys":[{"frame":0,"position":[5,3,7],"target":[0,1,0],"fov":45,"roll":0}]}}
      """); }
    @Test void cannonTargetsOnlyExistingCastles() throws Exception {
        var doc=document();var castle=((ObjectNode)doc.path("objects").get(0)).deepCopy();castle.put("id","gate");castle.put("kind","castle");
        var cannon=castle.deepCopy();cannon.put("id","cannon");cannon.put("kind","cannon");cannon.put("fireFrame",168);cannon.put("targetId","gate");
        ((com.fasterxml.jackson.databind.node.ArrayNode)doc.path("objects")).add(castle).add(cannon);
        assertDoesNotThrow(()->ShortDramaDirectorService.validate(doc));
        cannon.put("targetId","actor-1");assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(doc));
        cannon.put("targetId","missing");assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(doc));
    }
    @Test void persistsIndependentDocumentsAndRejectsStaleWrites() throws Exception {
        var service = new ShortDramaDirectorService(mock(ShortDramaSoundService.class),root);
        var doc = document(); assertNull(service.read(1L,"scene-a",2L).document());
        service.save(1L,"scene-a",2L,new ShortDramaDirectorService.Director(0,doc));
        assertEquals(doc,new ShortDramaDirectorService(mock(ShortDramaSoundService.class),root).read(1L,"scene-a",2L).document());
        assertThrows(IllegalArgumentException.class,()->service.save(1L,"scene-a",2L,new ShortDramaDirectorService.Director(0,doc)));
        assertEquals(1,service.read(1L,"scene-a",2L).revision()); assertNull(service.read(1L,"scene-b",2L).document());
    }
    @Test void ownershipAndSafePathsAreEnforced() throws Exception {
        var owners=mock(ShortDramaSoundService.class); doThrow(new IllegalArgumentException("无项目权限")).when(owners).owner(1L,3L);
        var service=new ShortDramaDirectorService(owners,root); var request=new ShortDramaDirectorService.Director(0,document());
        assertThrows(IllegalArgumentException.class,()->service.read(1L,"scene",3L));
        assertThrows(IllegalArgumentException.class,()->service.save(1L,"scene",3L,request));
        assertThrows(IllegalArgumentException.class,()->service.save(1L,"../../escape",2L,request));
        assertNull(service.read(1L,"scene",2L).document());
    }
    @Test void badKeyTimesAndCameraGeometryCannotBeSaved() throws Exception {
        var doc=document(); ((ObjectNode)doc.path("objects").get(0).path("keys").get(1)).put("frame",192);
        final var badFrame=doc; assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(badFrame));
        doc=document(); ((ObjectNode)doc.path("camera").path("keys").get(0)).set("target",doc.path("camera").path("keys").get(0).path("position").deepCopy());
        final var badCamera=doc; assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(badCamera));
    }
    @Test void timedActionsJointOffsetsAndCameraFollowingPersist() throws Exception {
        var doc=document();var actor=(ObjectNode)doc.path("objects").get(0);
        actor.put("autoFace",true); actor.set("clips",json.readTree("""
          [{"id":"clip-1","action":"point","start":24,"end":96,"speed":0.8,"strength":0.7,"blend":6}]
          """));
        ((ObjectNode)actor.path("keys").get(0)).set("pose",json.readTree("{\"rightElbow\":[-30,0,0],\"head\":[0,40,0],\"height\":-0.1}"));
        ((ObjectNode)actor.path("keys").get(0)).put("ease","smooth");
        ((ObjectNode)doc.path("camera")).set("follow",json.readTree("{\"objectId\":\"actor-1\",\"mode\":\"follow\",\"offset\":[0,2,5],\"height\":1.3}"));
        var service=new ShortDramaDirectorService(mock(ShortDramaSoundService.class),root);
        service.save(1L,"performance",2L,new ShortDramaDirectorService.Director(0,doc));
        assertEquals(doc,service.read(1L,"performance",2L).document());
    }
    @Test void malformedPerformanceDataCannotReplaceTheSavedDocument() throws Exception {
        var doc=document();var actor=(ObjectNode)doc.path("objects").get(0);
        actor.set("clips",json.readTree("""
          [{"id":"a","action":"wave","start":0,"end":72,"speed":1,"strength":1,"blend":6},
           {"id":"b","action":"nod","start":60,"end":120,"speed":1,"strength":1,"blend":6}]
          """));
        assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(doc));
        actor.remove("clips");((ObjectNode)actor.path("keys").get(0)).set("pose",json.readTree("{\"head\":[0,181,0]}"));
        assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(doc));
        ((ObjectNode)actor.path("keys").get(0)).remove("pose");actor.put("lookAtId","missing");
        assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(doc));
        actor.remove("lookAtId");((ObjectNode)doc.path("camera")).set("follow",json.readTree("{\"objectId\":\"actor-1\",\"mode\":\"follow\",\"offset\":[0,1.3,0],\"height\":1.3}"));
        assertThrows(IllegalArgumentException.class,()->ShortDramaDirectorService.validate(doc));
    }
}
