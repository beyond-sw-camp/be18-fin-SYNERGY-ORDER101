package com.synerge.order101.user.model.entity;

public enum Role {

        STORE_ADMIN("가맹점주"),
        SYSTEM("SYSTEM"),
        HQ("본사직원"),
        HQ_ADMIN("본사관리자");

        private final String label;

        Role(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }


}
