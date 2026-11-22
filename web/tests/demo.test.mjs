import test from 'node:test';import assert from 'node:assert/strict';
import {samples,resultFor,addStory,removeStory} from '../demo.mjs';
test('custom input cannot inherit an example verdict',()=>{assert.equal(resultFor('',samples[0].text),null);assert.equal(resultFor('kyc','my private draft'),null);});
test('editing any example invalidates the authored result',()=>{for(const sample of samples){assert.equal(resultFor(sample.id,sample.text),sample);assert.equal(resultFor(sample.id,sample.text+' changed'),null);if(sample.quote)assert.ok(sample.text.includes(sample.quote));}});
test('story items remain unique, in order and removable',()=>{assert.deepEqual(addStory(['offer'],'payment'),['offer','payment']);assert.deepEqual(addStory(['offer','payment'],'offer'),['offer','payment']);assert.deepEqual(removeStory(['offer','payment'],'payment'),['offer']);});
