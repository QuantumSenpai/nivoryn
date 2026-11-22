import {test} from 'node:test';
import assert from 'node:assert/strict';
import {productExamples,exampleFor,storyProgress} from '../product-examples.mjs';
test('every kind has matching evidence and honest provenance',()=>{
 assert.deepEqual(productExamples.map(e=>e.kind),['link','message','screenshot','document']);
 for(const e of productExamples){assert.ok(e.input.includes(e.quote));assert.ok(e.unknown);assert.match(e.mode,/Authored/);}
 assert.equal(exampleFor('message').evidence,'Upfront payment pressure');
 assert.equal(exampleFor('custom input'),null);
});
test('scroll state clamps, reverses and has no independent time input',()=>{
 assert.equal(storyProgress(-1),0);assert.equal(storyProgress(2),1);assert.equal(storyProgress(NaN),0);
 assert.deepEqual([.1,.8,.4,0].map(storyProgress),[.1,.8,.4,0]);
});
