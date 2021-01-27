Convolve {
	classvar fftSize=65536, <>convBuf;

	*makeWhole {|arrays|
		var temp;
		temp = ((arrays[0].size/fftSize).ceil*fftSize).asInteger-arrays[0].size;
		arrays = arrays.collect{|array| array.addAll(Array.fill(temp, {0}))};
		^arrays
	}

	*doit {|server, bufA, bufB, action|
		var aArrays, bArrays, final, numOutChans;

		action ?? {action=={"".postln;"convolved!".postln;}};

		bufA.loadToFloatArray(action:{|array|
			aArrays = this.makeWhole(array.clump(bufA.numChannels).flop);

			bufB.loadToFloatArray(action:{|arrayb|
				bArrays = this.makeWhole(arrayb.clump(bufB.numChannels).flop);

				numOutChans = max(bufA.numChannels, bufB.numChannels);

				final = List.fill(numOutChans, {List.fill(aArrays[0].size+bArrays[0].size-1, {0})});
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
						}
					}
				};
				final = final.flop.flatten.asArray;
				final=final/final[final.maxIndex];
				convBuf = Buffer.loadCollection(server, final, numOutChans, {|buf| action.value(buf)});
		})})
	}

	*fileConvolve {|server, source, impulse, action|

		server.waitForBoot{

			/*			if(SoundFile.openRead(impulse).numFrames>(2**20)){"impulse too big. must be fewer than 2**20 samples (23 seconds at 44100)".postln}{*/
			"convolving".postln;
			Buffer.read(server, source, action:{|bufA|
				Buffer.read(server, impulse, action:{|bufB|
					this.doit(server, bufA, bufB, action);
			})})
			//}
		}
	}

	*bufConvolve {|server, sourceBuf, impulseBuf, action|
		if(impulseBuf.numFrames>(2**20)){"impulse too big. must be fewer than 2**20 samples".postln}{
			"convolving".postln;
			this.doit(server, sourceBuf, impulseBuf, action);
		}
	}
}
