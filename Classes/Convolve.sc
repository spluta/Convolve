Convolve {
	classvar <>convBuf;

	*makeWhole {|arrays, fftSize|
		var temp;
		temp = ((arrays[0].size/fftSize).ceil*fftSize).asInteger-arrays[0].size;
		arrays = arrays.collect{|array| array.addAll(Array.fill(temp, {0}))};
		^arrays
	}

	*doit {|server, bufA, bufB, action|
		var aArrays, bArrays, final, finalB, numOutChans;

		action ?? {action=={"".postln;"convolved!".postln;}};

		bufA.loadToFloatArray(action:{|array|


			bufB.loadToFloatArray(action:{|arrayb|
				var fftSize = min(bufA.numFrames.nextPowerOfTwo.asInteger, bufB.numFrames.nextPowerOfTwo.asInteger);
				if(fftSize>(2**17)){fftSize = 2**17};
				("fftSize: "++fftSize).postln;
				aArrays = this.makeWhole(array.clump(bufA.numChannels).flop, fftSize);
				bArrays = this.makeWhole(arrayb.clump(bufB.numChannels).flop, fftSize);

				numOutChans = max(bufA.numChannels, bufB.numChannels);
				numOutChans.postln;

				final = List.fill(numOutChans, {List.fill(aArrays[0].size+bArrays[0].size, {0})});

				numOutChans.do{|chan|
					"".postln;
					("chan "++chan).postln;
					"good things come to those who wait".postln;
					(aArrays[0].size/(fftSize)).do{|i|
						var conv;
						var seg = aArrays[(chan%bufA.numChannels)].copyRange((fftSize*i).asInteger, (fftSize*(i+1)).asInteger);
						(bArrays[0].size/(fftSize)).do{|bi|
							var bseg = bArrays[(chan%bufB.numChannels)].as(Signal).copyRange((fftSize*bi).asInteger, (fftSize*(bi+1)).asInteger);
							".".post;
							conv = seg.as(Signal).convolve(bseg);
							conv.do{|convVal, i2| final[chan].put(fftSize*(i+bi)+i2, final[chan][fftSize*(i+bi)+i2]+convVal)};
						};
					};
				};
				finalB = Array.newClear(final[0].size*numOutChans);
				final = final.asArray.flop;
				final.do{|item, i| item.do{|item2, i2| finalB=finalB.put(i*2+i2, item2)}};
				finalB=finalB/finalB[finalB.maxIndex];
				finalB.postln;
				convBuf = Buffer.loadCollection(server, finalB, numOutChans, {|buf| "".postln; buf.postln; action.value(buf)});
		})})
	}

	*fileConvolve {|server, source, impulse, action|

		server.waitForBoot{
			"convolving".postln;
			Buffer.read(server, source, action:{|bufA|
				Buffer.read(server, impulse, action:{|bufB|
					this.doit(server, bufA, bufB, action);
			})})
			//}
		}
	}

	*bufConvolve {|server, sourceBuf, impulseBuf, action|
		"convolving".postln;
		this.doit(server, sourceBuf, impulseBuf, action);
	}
}
