"use strict";
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, { get: all[name], enumerable: true });
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);
var items_exports = {};
__export(items_exports, {
  Items: () => Items
});
module.exports = __toCommonJS(items_exports);

const Items = {
  actionheromask: {
    name: "Action Hero Mask",
    fling: {
      basePower: 0
    },
    spritenum: 2001,
    onBasePowerPriority: 15,
    onBasePower(basePower, user, target, move) {
      if (user.baseSpecies.num === 2001) {
        return this.chainModify(ACTION_HERO_MASK_POWER_CONFIG);
      }
    },
    itemUser: ["Xiaoxin"],
    num: 2001,
    gen: 8,
    isNonstandard: "Past"
  }
};
//# sourceMappingURL=items.js.map 