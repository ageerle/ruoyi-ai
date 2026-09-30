package org.ruoyi.service.shortdrama.impl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev") class ShortDramaParallelTest {
    @Test void runsThreeScenesConcurrentlyButKeepsScriptOrder() {
        var latch=new CountDownLatch(3);var active=new AtomicInteger();var max=new AtomicInteger();
        var result=ShortDramaParallel.mapOrdered(List.of(1,2,3,4,5,6),3,n->{
            max.accumulateAndGet(active.incrementAndGet(),Math::max);latch.countDown();
            try {assertTrue(latch.await(2,TimeUnit.SECONDS));Thread.sleep((7-n)*8L);return n*10;}
            catch(InterruptedException e){throw new RuntimeException(e);}
            finally{active.decrementAndGet();}
        });
        assertEquals(List.of(10,20,30,40,50,60),result);assertEquals(3,max.get());
    }
    @Test void aFailedSceneDoesNotProducePartialSuccessfulFilm() {
        assertThrows(IllegalStateException.class,()->ShortDramaParallel.mapOrdered(List.of(1,2),2,n->{if(n==2)throw new IllegalArgumentException("scene failed");return n;}));
    }
}
