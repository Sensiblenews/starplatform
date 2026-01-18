"use strict";
(self["webpackChunkapp"] = self["webpackChunkapp"] || []).push([["src_app_pages_more_more_module_ts"],{

/***/ 6163:
/*!***************************************************!*\
  !*** ./src/app/pages/more/more-routing.module.ts ***!
  \***************************************************/
/***/ ((__unused_webpack_module, __webpack_exports__, __webpack_require__) => {

__webpack_require__.r(__webpack_exports__);
/* harmony export */ __webpack_require__.d(__webpack_exports__, {
/* harmony export */   "MorePageRoutingModule": () => (/* binding */ MorePageRoutingModule)
/* harmony export */ });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! tslib */ 4929);
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/core */ 3184);
/* harmony import */ var _angular_router__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! @angular/router */ 2816);
/* harmony import */ var _more_page__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./more.page */ 5117);




const routes = [
    {
        path: '',
        component: _more_page__WEBPACK_IMPORTED_MODULE_0__.MorePage
    }
];
let MorePageRoutingModule = class MorePageRoutingModule {
};
MorePageRoutingModule = (0,tslib__WEBPACK_IMPORTED_MODULE_1__.__decorate)([
    (0,_angular_core__WEBPACK_IMPORTED_MODULE_2__.NgModule)({
        imports: [_angular_router__WEBPACK_IMPORTED_MODULE_3__.RouterModule.forChild(routes)],
        exports: [_angular_router__WEBPACK_IMPORTED_MODULE_3__.RouterModule],
    })
], MorePageRoutingModule);



/***/ }),

/***/ 1911:
/*!*******************************************!*\
  !*** ./src/app/pages/more/more.module.ts ***!
  \*******************************************/
/***/ ((__unused_webpack_module, __webpack_exports__, __webpack_require__) => {

__webpack_require__.r(__webpack_exports__);
/* harmony export */ __webpack_require__.d(__webpack_exports__, {
/* harmony export */   "MorePageModule": () => (/* binding */ MorePageModule)
/* harmony export */ });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! tslib */ 4929);
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! @angular/core */ 3184);
/* harmony import */ var _angular_common__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! @angular/common */ 6362);
/* harmony import */ var _angular_forms__WEBPACK_IMPORTED_MODULE_5__ = __webpack_require__(/*! @angular/forms */ 587);
/* harmony import */ var _ionic_angular__WEBPACK_IMPORTED_MODULE_6__ = __webpack_require__(/*! @ionic/angular */ 3819);
/* harmony import */ var _more_routing_module__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./more-routing.module */ 6163);
/* harmony import */ var _more_page__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ./more.page */ 5117);







let MorePageModule = class MorePageModule {
};
MorePageModule = (0,tslib__WEBPACK_IMPORTED_MODULE_2__.__decorate)([
    (0,_angular_core__WEBPACK_IMPORTED_MODULE_3__.NgModule)({
        imports: [
            _angular_common__WEBPACK_IMPORTED_MODULE_4__.CommonModule,
            _angular_forms__WEBPACK_IMPORTED_MODULE_5__.FormsModule,
            _ionic_angular__WEBPACK_IMPORTED_MODULE_6__.IonicModule,
            _more_routing_module__WEBPACK_IMPORTED_MODULE_0__.MorePageRoutingModule
        ],
        declarations: [_more_page__WEBPACK_IMPORTED_MODULE_1__.MorePage]
    })
], MorePageModule);



/***/ }),

/***/ 5117:
/*!*****************************************!*\
  !*** ./src/app/pages/more/more.page.ts ***!
  \*****************************************/
/***/ ((__unused_webpack_module, __webpack_exports__, __webpack_require__) => {

__webpack_require__.r(__webpack_exports__);
/* harmony export */ __webpack_require__.d(__webpack_exports__, {
/* harmony export */   "MorePage": () => (/* binding */ MorePage)
/* harmony export */ });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! tslib */ 4929);
/* harmony import */ var _more_page_html_ngResource__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./more.page.html?ngResource */ 7788);
/* harmony import */ var _more_page_scss_ngResource__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ./more.page.scss?ngResource */ 954);
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_5__ = __webpack_require__(/*! @angular/core */ 3184);
/* harmony import */ var _ionic_angular__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! @ionic/angular */ 3819);
/* harmony import */ var _awesome_cordova_plugins_social_sharing_ngx__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @awesome-cordova-plugins/social-sharing/ngx */ 1952);






let MorePage = class MorePage {
    constructor(navController, socialSharing) {
        this.navController = navController;
        this.socialSharing = socialSharing;
    }
    ngOnInit() {
    }
    moveBack() { this.navController.back(); }
    goShareApp() { this.socialSharing.share('‘마냥사냥’ 앱을 다운받아 보세요~~  https://witch-hunting.com/appUrl'); }
};
MorePage.ctorParameters = () => [
    { type: _ionic_angular__WEBPACK_IMPORTED_MODULE_3__.NavController },
    { type: _awesome_cordova_plugins_social_sharing_ngx__WEBPACK_IMPORTED_MODULE_2__.SocialSharing }
];
MorePage = (0,tslib__WEBPACK_IMPORTED_MODULE_4__.__decorate)([
    (0,_angular_core__WEBPACK_IMPORTED_MODULE_5__.Component)({
        selector: 'app-more',
        template: _more_page_html_ngResource__WEBPACK_IMPORTED_MODULE_0__,
        styles: [_more_page_scss_ngResource__WEBPACK_IMPORTED_MODULE_1__]
    })
], MorePage);



/***/ }),

/***/ 954:
/*!******************************************************!*\
  !*** ./src/app/pages/more/more.page.scss?ngResource ***!
  \******************************************************/
/***/ ((module) => {

module.exports = "ion-content .gusetModeAL {\n  background-color: #f79a19;\n  color: #ffffff;\n  text-align: center;\n  padding: 15px;\n}\nion-content .moreMenuSection {\n  background-color: #eeeeee;\n  height: calc(100% - 51px);\n  padding: 8px;\n  display: flex;\n  flex-direction: column;\n}\nion-content .moreMenuSection .btn_wrap {\n  flex: 1;\n  margin-bottom: 8px;\n  display: flex;\n  flex-direction: column;\n}\nion-content .moreMenuSection .btn_wrap .btn_wrap_sub {\n  flex: 1;\n  display: flex;\n}\nion-content .moreMenuSection .btn_wrap .btn_wrap_sub:first-child {\n  margin-bottom: 8px;\n}\nion-content .moreMenuSection .btn_wrap .btn_wrap_sub .card {\n  background-color: #ffffff;\n  flex: 1;\n  margin-left: 8px;\n  display: flex;\n  flex-direction: column;\n  align-items: center;\n  justify-content: flex-end;\n  position: relative;\n}\nion-content .moreMenuSection .btn_wrap .btn_wrap_sub .card:first-child {\n  margin-left: 0;\n}\nion-content .moreMenuSection .btn_wrap .btn_wrap_sub .card .icon {\n  width: 60%;\n  position: absolute;\n  top: 50%;\n  left: 50%;\n  transform: translate(-50%, -50%);\n}\nion-content .moreMenuSection .btn_wrap .btn_wrap_sub .card .label {\n  color: #f79a19;\n  font-size: 0.7em;\n  font-weight: bold;\n  margin-bottom: 8px;\n}\nion-content .moreMenuSection ion-button {\n  font-weight: bold;\n  --border-radius: 50px;\n  width: 80%;\n  margin: 4px auto;\n}\n/*# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIm1vcmUucGFnZS5zY3NzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiJBQUNJO0VBQ0kseUJBQUE7RUFDQSxjQUFBO0VBQ0Esa0JBQUE7RUFDQSxhQUFBO0FBQVI7QUFFSTtFQUNJLHlCQUFBO0VBQ0EseUJBQUE7RUFDQSxZQUFBO0VBQ0EsYUFBQTtFQUNBLHNCQUFBO0FBQVI7QUFDUTtFQUNJLE9BQUE7RUFDQSxrQkFBQTtFQUNBLGFBQUE7RUFDQSxzQkFBQTtBQUNaO0FBQVk7RUFDSSxPQUFBO0VBQ0EsYUFBQTtBQUVoQjtBQURnQjtFQUFlLGtCQUFBO0FBSS9CO0FBSGdCO0VBQ0kseUJBQUE7RUFDQSxPQUFBO0VBQ0EsZ0JBQUE7RUFFQSxhQUFBO0VBQ0Esc0JBQUE7RUFDQSxtQkFBQTtFQUNBLHlCQUFBO0VBQ0Esa0JBQUE7QUFJcEI7QUFIb0I7RUFBZSxjQUFBO0FBTW5DO0FBTG9CO0VBQ0ksVUFBQTtFQUNBLGtCQUFBO0VBQ0EsUUFBQTtFQUNBLFNBQUE7RUFDQSxnQ0FBQTtBQU94QjtBQUxvQjtFQUNJLGNBQUE7RUFDQSxnQkFBQTtFQUNBLGlCQUFBO0VBQ0Esa0JBQUE7QUFPeEI7QUFGUTtFQUNJLGlCQUFBO0VBQ0EscUJBQUE7RUFDQSxVQUFBO0VBQ0EsZ0JBQUE7QUFJWiIsImZpbGUiOiJtb3JlLnBhZ2Uuc2NzcyIsInNvdXJjZXNDb250ZW50IjpbImlvbi1jb250ZW50e1xyXG4gICAgLmd1c2V0TW9kZUFMe1xyXG4gICAgICAgIGJhY2tncm91bmQtY29sb3I6ICNmNzlhMTk7XHJcbiAgICAgICAgY29sb3I6ICNmZmZmZmY7XHJcbiAgICAgICAgdGV4dC1hbGlnbjogY2VudGVyO1xyXG4gICAgICAgIHBhZGRpbmc6IDE1cHg7XHJcbiAgICB9XHJcbiAgICAubW9yZU1lbnVTZWN0aW9ue1xyXG4gICAgICAgIGJhY2tncm91bmQtY29sb3I6ICNlZWVlZWU7XHJcbiAgICAgICAgaGVpZ2h0OiBjYWxjKDEwMCUgLSA1MXB4KTtcclxuICAgICAgICBwYWRkaW5nOiA4cHg7XHJcbiAgICAgICAgZGlzcGxheTogZmxleDtcclxuICAgICAgICBmbGV4LWRpcmVjdGlvbjogY29sdW1uO1xyXG4gICAgICAgIC5idG5fd3JhcHtcclxuICAgICAgICAgICAgZmxleDogMTtcclxuICAgICAgICAgICAgbWFyZ2luLWJvdHRvbTogOHB4O1xyXG4gICAgICAgICAgICBkaXNwbGF5OiBmbGV4O1xyXG4gICAgICAgICAgICBmbGV4LWRpcmVjdGlvbjogY29sdW1uO1xyXG4gICAgICAgICAgICAuYnRuX3dyYXBfc3Vie1xyXG4gICAgICAgICAgICAgICAgZmxleDogMTtcclxuICAgICAgICAgICAgICAgIGRpc3BsYXk6IGZsZXg7XHJcbiAgICAgICAgICAgICAgICAmOmZpcnN0LWNoaWxkeyBtYXJnaW4tYm90dG9tOiA4cHg7IH1cclxuICAgICAgICAgICAgICAgIC5jYXJke1xyXG4gICAgICAgICAgICAgICAgICAgIGJhY2tncm91bmQtY29sb3I6ICNmZmZmZmY7XHJcbiAgICAgICAgICAgICAgICAgICAgZmxleDogMTtcclxuICAgICAgICAgICAgICAgICAgICBtYXJnaW4tbGVmdDogOHB4O1xyXG4gICAgICAgICAgICAgICAgICAgIC8vIHBhZGRpbmc6IDhweDtcclxuICAgICAgICAgICAgICAgICAgICBkaXNwbGF5OiBmbGV4O1xyXG4gICAgICAgICAgICAgICAgICAgIGZsZXgtZGlyZWN0aW9uOiBjb2x1bW47XHJcbiAgICAgICAgICAgICAgICAgICAgYWxpZ24taXRlbXM6IGNlbnRlcjtcclxuICAgICAgICAgICAgICAgICAgICBqdXN0aWZ5LWNvbnRlbnQ6IGZsZXgtZW5kO1xyXG4gICAgICAgICAgICAgICAgICAgIHBvc2l0aW9uOiByZWxhdGl2ZTtcclxuICAgICAgICAgICAgICAgICAgICAmOmZpcnN0LWNoaWxkeyBtYXJnaW4tbGVmdDogMDsgfVxyXG4gICAgICAgICAgICAgICAgICAgIC5pY29ue1xyXG4gICAgICAgICAgICAgICAgICAgICAgICB3aWR0aDogNjAlO1xyXG4gICAgICAgICAgICAgICAgICAgICAgICBwb3NpdGlvbjogYWJzb2x1dGU7XHJcbiAgICAgICAgICAgICAgICAgICAgICAgIHRvcDogNTAlO1xyXG4gICAgICAgICAgICAgICAgICAgICAgICBsZWZ0OiA1MCU7XHJcbiAgICAgICAgICAgICAgICAgICAgICAgIHRyYW5zZm9ybTogdHJhbnNsYXRlKC01MCUsIC01MCUpO1xyXG4gICAgICAgICAgICAgICAgICAgIH1cclxuICAgICAgICAgICAgICAgICAgICAubGFiZWx7XHJcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbG9yOiAjZjc5YTE5O1xyXG4gICAgICAgICAgICAgICAgICAgICAgICBmb250LXNpemU6IDAuN2VtO1xyXG4gICAgICAgICAgICAgICAgICAgICAgICBmb250LXdlaWdodDogYm9sZDtcclxuICAgICAgICAgICAgICAgICAgICAgICAgbWFyZ2luLWJvdHRvbTogOHB4O1xyXG4gICAgICAgICAgICAgICAgICAgIH1cclxuICAgICAgICAgICAgICAgIH1cclxuICAgICAgICAgICAgfVxyXG4gICAgICAgIH1cclxuICAgICAgICBpb24tYnV0dG9ue1xyXG4gICAgICAgICAgICBmb250LXdlaWdodDogYm9sZDtcclxuICAgICAgICAgICAgLS1ib3JkZXItcmFkaXVzOiA1MHB4O1xyXG4gICAgICAgICAgICB3aWR0aDogODAlO1xyXG4gICAgICAgICAgICBtYXJnaW46IDRweCBhdXRvO1xyXG4gICAgICAgIH1cclxuICAgIH1cclxufSJdfQ== */";

/***/ }),

/***/ 7788:
/*!******************************************************!*\
  !*** ./src/app/pages/more/more.page.html?ngResource ***!
  \******************************************************/
/***/ ((module) => {

module.exports = "<ion-header [translucent]=\"true\">\n  <ion-toolbar color=\"primary\">\n    <ion-buttons slot=\"start\" (click)=\"moveBack()\">\n      <ion-button>\n        <ion-icon name=\"chevron-back\"></ion-icon>\n        <ion-label>Back</ion-label>\n      </ion-button>\n    </ion-buttons>\n    <ion-title>\n      더보기\n    </ion-title>\n  </ion-toolbar>\n</ion-header>\n\n<ion-content>\n  <div class=\"gusetModeAL\">\n    게스트 모드(guest mode)입니다.\n  </div>\n  <div class=\"moreMenuSection\">\n    <div class=\"btn_wrap\">\n      <div class=\"btn_wrap_sub\">\n        <div class=\"card\">\n          <div class=\"icon\">\n            <img src=\"../../../assets/img/01.png\"/>\n          </div>\n          <div class=\"label\">사용설명서</div>\n        </div>\n        <div class=\"card\">\n          <div class=\"icon\">\n            <img src=\"../../../assets/img/02.png\"/>\n          </div>\n          <div class=\"label\">홍보 존</div>\n        </div>\n        <div class=\"card\">\n          <div class=\"icon\">\n            <img src=\"../../../assets/img/03.png\"/>\n          </div>\n          <div class=\"label\">논리구구단</div>\n        </div>\n        <div class=\"card\">\n          <div class=\"icon\">\n            <img src=\"../../../assets/img/04.png\"/>\n          </div>\n          <div class=\"label\">스토리</div>\n        </div>\n      </div>\n      <div class=\"btn_wrap_sub\">\n        <div class=\"card\">\n          <div class=\"icon\">\n            <img src=\"../../../assets/img/05.png\"/>\n          </div>\n          <div class=\"label\">논리500</div>\n        </div>\n        <div class=\"card\">\n          <div class=\"icon\">\n            <img src=\"../../../assets/img/06.png\"/>\n          </div>\n          <div class=\"label\">ID 신청</div>\n        </div>\n        <div class=\"card\">\n          <div class=\"icon\">\n            <img src=\"../../../assets/img/07.png\"/>\n          </div>\n          <div class=\"label\">FAQ</div>\n        </div>\n        <div class=\"card\" (click)=\"goShareApp()\">\n          <div class=\"icon\">\n            <img src=\"../../../assets/img/08.png\"/>\n          </div>\n          <div class=\"label\">앱 공유하기</div>\n        </div>\n      </div>\n    </div>\n    <ion-button>\n      별똥별\n      <ion-icon slot=\"end\" name=\"arrow-forward\"></ion-icon>\n    </ion-button>\n  </div>\n</ion-content>\n";

/***/ })

}]);
//# sourceMappingURL=src_app_pages_more_more_module_ts.js.map