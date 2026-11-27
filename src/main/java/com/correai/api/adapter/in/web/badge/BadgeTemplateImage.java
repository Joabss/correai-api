package com.correai.api.adapter.in.web.badge;

import com.correai.api.domain.model.badge.BadgeTemplate;

final class BadgeTemplateImage {
    private BadgeTemplateImage() {}

    static String render(BadgeTemplate template) {
        String icon = switch (template.icon()) {
            case FOOTPRINT -> """
                    <g fill="#fff"><ellipse cx="104" cy="114" rx="15" ry="24" transform="rotate(-12 104 114)"/><ellipse cx="98" cy="152" rx="11" ry="12"/><ellipse cx="154" cy="96" rx="15" ry="24" transform="rotate(12 154 96)"/><ellipse cx="160" cy="134" rx="11" ry="12"/></g>
                    """;
            case FIVE_K -> """
                    <text x="128" y="150" text-anchor="middle" font-family="Arial, Helvetica, sans-serif" font-weight="900" font-size="66" fill="#fff">5K</text><path d="M92 170 H164" stroke="#fff" stroke-width="6" stroke-linecap="round" stroke-dasharray="10 10"/>
                    """;
        };
        return """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256" width="256" height="256">
                <polygon points="128,16 225,72 225,184 128,240 31,184 31,72" fill="#333" stroke="#333" stroke-width="18" stroke-linejoin="round"/>
                <polygon points="128,24 218,76 218,180 128,232 38,180 38,76" fill="%s" stroke="%s" stroke-width="6" stroke-linejoin="round"/>
                <polygon points="128,40 204,84 204,172 128,216 52,172 52,84" fill="none" stroke="#fff" stroke-opacity=".35" stroke-width="3" stroke-linejoin="round"/>
                <path d="M60 92 L128 52 L196 92 C170 80 150 76 128 76 C106 76 86 80 60 92 Z" fill="#fff" opacity=".18"/>
                %s</svg>
                """.formatted(template.color(), template.color(), icon);
    }
}
