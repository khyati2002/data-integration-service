/*
 * Copyright (c) Applicate AI 2022. All rights reserved.
 *
 */
package com.applicate.services.channelkart.templates;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.helper.StringHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * @author : Jinu
 * Date    : 6/26/2020
 **/
@Service
public class TemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    private Handlebars handlebars;

    public TemplateEngine() {
        handlebars = createHandleBar();
    }

    public String applyInline(String templateString, Object contextData) {
        try {
            return this.handlebars
                    .compileInline(templateString)
                    .apply(contextData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Handlebars createHandleBar() {
        Handlebars hBars = new Handlebars()
                .with(EscapingStrategy.NOOP);
        StringHelpers.register(hBars);
        Arrays.stream(ConditionalHelpers.values()).forEach(helper -> hBars.registerHelper(helper.name(), helper));


        return hBars;
    }

}
