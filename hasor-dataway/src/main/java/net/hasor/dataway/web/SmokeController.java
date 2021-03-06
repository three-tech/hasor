/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataway.web;
import com.alibaba.fastjson.JSON;
import net.hasor.core.spi.SpiTrigger;
import net.hasor.dataql.QueryResult;
import net.hasor.dataql.domain.ObjectModel;
import net.hasor.dataway.config.DatawayUtils;
import net.hasor.dataway.config.MappingToUrl;
import net.hasor.dataway.config.Result;
import net.hasor.dataway.daos.ApiDetailQuery;
import net.hasor.dataway.service.ApiCallService;
import net.hasor.dataway.spi.ApiInfo;
import net.hasor.dataway.spi.ParseParameterChainSpi;
import net.hasor.utils.StringUtils;
import net.hasor.web.Invoker;
import net.hasor.web.annotation.Post;
import net.hasor.web.annotation.QueryParameter;
import net.hasor.web.annotation.RequestBody;
import net.hasor.web.objects.JsonRenderEngine;
import net.hasor.web.render.RenderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 冒烟测试（于执行不同的是，冒烟测试的执行脚本和脚本类型信息来源于已保存的）
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-03-24
 */
@MappingToUrl("/api/smoke")
@RenderType(value = "json", engineType = JsonRenderEngine.class)
public class SmokeController extends BasicController {
    protected static Logger         logger = LoggerFactory.getLogger(SmokeController.class);
    @Inject
    private          ApiCallService apiCallService;
    @Inject
    private          SpiTrigger     spiTrigger;

    @Post
    public Result<Map<String, Object>> doSmoke(Invoker invoker, @QueryParameter("id") String apiId, @RequestBody() Map<String, Object> requestBody) throws IOException {
        if (!apiId.equalsIgnoreCase(requestBody.get("id").toString())) {
            throw new IllegalArgumentException("id Parameters of the ambiguity.");
        }
        //
        // .查询接口数据
        QueryResult queryDetail = new ApiDetailQuery(this.dataQL).execute(new HashMap<String, String>() {{
            put("apiId", apiId);
        }});
        ObjectModel objectModel = ((ObjectModel) queryDetail.getData());
        //
        // .获取API信息
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiID(apiId);
        apiInfo.setReleaseID("");
        apiInfo.setMethod(objectModel.getValue("select").asString());
        apiInfo.setApiPath(objectModel.getValue("path").asString());
        String strCodeType = objectModel.getValue("codeType").asString();
        String strCodeValue = objectModel.getObject("codeInfo").getValue("codeValue").asString();
        //
        // .准备参数
        String jsonParamValue = objectModel.getObject("codeInfo").getValue("requestBody").asString();
        jsonParamValue = (StringUtils.isBlank(jsonParamValue)) ? "{}" : jsonParamValue;
        Map<String, Object> jsonParamOri = JSON.parseObject(jsonParamValue);
        Map<String, Object> jsonParam = this.spiTrigger.chainSpi(ParseParameterChainSpi.class, (listener, lastResult) -> {
            return listener.parseParameter(true, apiInfo, invoker, lastResult);
        }, jsonParamOri);
        apiInfo.setParameterMap(JSON.parseObject(jsonParamValue));
        //
        // .如果是 SQL 还需要进行代码替换
        if ("sql".equalsIgnoreCase(strCodeType)) {
            strCodeValue = DatawayUtils.evalCodeValueForSQL(strCodeValue, jsonParam);
        }
        //
        // .执行调用
        try {
            Map<String, Object> objectMap = this.apiCallService.doCall(apiInfo, strCodeValue, jsonParam);
            this.updateSchema(apiId, jsonParamOri, objectMap);
            return Result.of(objectMap);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return DatawayUtils.exceptionToResult(e);
        }
    }

    private void updateSchema(String apiID, Map<String, Object> requestData, Map<String, Object> responseData) {
        //
    }
}