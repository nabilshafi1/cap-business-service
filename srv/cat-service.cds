using my.bookshop as my from '../db/data-model';

service cloud.sdk.capng {
     entity CapBusinessPartner as projection on my.CapBusinessPartner;
}
