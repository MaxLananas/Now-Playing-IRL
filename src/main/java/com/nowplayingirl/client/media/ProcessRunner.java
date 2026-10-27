package com.nowplayingirl.client.media;
import java.nio.charset.StandardCharsets; import java.util.List; import java.util.concurrent.*;
/** Runs a helper process with a hard timeout (never hangs the polling thread). */
public final class ProcessRunner {
    private ProcessRunner(){}
    public static String run(List<String> cmd, long timeoutMs){
        Process p=null;
        try{ p=new ProcessBuilder(cmd).redirectErrorStream(true).start();
            Process proc=p;
            CompletableFuture<byte[]> fut=CompletableFuture.supplyAsync(()->{ try{return proc.getInputStream().readAllBytes();}catch(Exception e){return new byte[0];} });
            if(!p.waitFor(timeoutMs,TimeUnit.MILLISECONDS)){ p.destroyForcibly(); return ""; }
            return new String(fut.get(200,TimeUnit.MILLISECONDS),StandardCharsets.UTF_8).trim();
        }catch(Exception e){ if(p!=null)p.destroyForcibly(); return ""; }
    }
}
