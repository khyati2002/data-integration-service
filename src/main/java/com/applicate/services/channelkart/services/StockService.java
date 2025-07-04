package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.jooq.impl.StockIntegration;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;


import java.io.IOException;
import java.util.Collection;

public class StockService extends AbstractCDMService<StockIntegration>{
    private transient HttpClient httpClient;

    @Override
    public Collection<StockIntegration> batchSave(Collection<StockIntegration> cdmObject) {
            httpClient = HttpClients.createDefault();


        String url = getStockBaseUrl() + "/consolidatedStock/bulkStockUpload";
        HttpPost request = new HttpPost(url);
        request.setHeader("Authorization", SecurityContextUtils.getToken());
        request.setHeader("lob", SecurityContextUtils.getLob());
        try {
            String json = new ObjectMapper().writeValueAsString(cdmObject);
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            httpClient.execute(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cdmObject;
    }

    private String getStockBaseUrl() {
        String env = SecurityContextUtils.getEnv();
        switch (env){
            case "demo": return "https://stocks-demo.salescode.ai";
            case "prod": return "https://stocks-prod.salescode.ai";
            default: return "https://stocks-uat.salescode.ai";
        }
    }

}
