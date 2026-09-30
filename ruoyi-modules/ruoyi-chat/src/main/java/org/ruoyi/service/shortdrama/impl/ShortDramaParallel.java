package org.ruoyi.service.shortdrama.impl;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/** Bounded parallelism, deterministic output order, fail without losing completed checkpoints. */
final class ShortDramaParallel {
    private ShortDramaParallel() { }
    static <T,R> List<R> mapOrdered(List<T> inputs,int concurrency,Function<T,R> fn) {
        if(inputs.isEmpty())return List.of();
        var pool=Executors.newFixedThreadPool(Math.min(concurrency,inputs.size()));
        List<Future<R>> jobs=new ArrayList<>();
        try {
            for(var input:inputs)jobs.add(pool.submit(()->fn.apply(input)));
            List<R> results=new ArrayList<>();
            ExecutionException failure=null;
            for(var job:jobs) {
                try { results.add(job.get()); }
                catch(ExecutionException e) { results.add(null); if(failure==null)failure=e; }
            }
            if(failure!=null)throw failure;
            return results;
        } catch(InterruptedException e) {Thread.currentThread().interrupt();throw new IllegalStateException("任务中断，已完成部分可恢复",e);}
        catch(ExecutionException e) {throw new IllegalStateException(e.getCause().getMessage(),e.getCause());}
        finally {pool.shutdownNow();}
    }
}
