//Copyright © 2021 Sam Pluta - sampluta.com
//Released under GPLv3 License

Convolve {
	classvar <>convBuf, <>convArray;

	*makeWhole {|arrays, fftSize|
		var temp;
		temp = ((arrays[0].size/fftSize).ceil*fftSize).asInteger-arrays[0].size;
		arrays = arrays.collect{|array| array.addAll(Array.fill(temp, {0}))};
		^arrays
	}

	*doit {|server, bufA, bufB, action|
		var aArrays, bArrays, numOutChans;

		action ?? {action=={"".postln;"convolved!".postln;}};

		bufA.loadToFloatArray(action:{|array|


			bufB.loadToFloatArray(action:{|arrayb|
				var fftSize = min(bufA.numFrames.nextPowerOfTwo.asInteger, bufB.numFrames.nextPowerOfTwo.asInteger);
				if(fftSize>(2**17)){fftSize = 2**17};
				("fftSize: "++fftSize).postln;
				aArrays = this.makeWhole(array.clump(bufA.numChannels).flop, fftSize);
				bArrays = this.makeWhole(arrayb.clump(bufB.numChannels).flop, fftSize);

				numOutChans = max(bufA.numChannels, bufB.numChannels);

				convArray = List.fill(numOutChans, {List.fill(aArrays[0].size+bArrays[0].size, {0})});

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
							conv.do{|convVal, i2| convArray[chan].put(fftSize*(i+bi)+i2, convArray[chan][fftSize*(i+bi)+i2]+convVal)};
						};
					};
				};
				if(convArray[0].size<(2**25.9)){
					convArray = this.flatten(convArray.asArray);
					numOutChans.postln;
					convArray=convArray/convArray[convArray.maxIndex];
					convBuf = Buffer.loadCollection(server, convArray, numOutChans, {|buf| "".postln; buf.postln; action.value(buf)});
				}{
					var maxVal;
					"".postln;
					"array too large. loading each channel buffer into its own buffer.".postln;
					maxVal = max(convArray.collect{|chan| chan[chan.maxIndex]});
					convBuf = convArray.collect{|finChan, i|
						finChan = finChan/maxVal;
						Buffer.loadCollection(server, finChan, 1, {|buf| if(i==(convArray.size-1)){"no action. buffer channels are available as convBuf.".postln}});
					};
				}
		})})
	}

	*flatten { |arr|
		var res;
		if(arr.size>1){
			arr = arr.flop;
			res = Array.newClear(arr.size * 2);
			arr.do { |pair, i|
				res.put(i * 2, pair[0]);
				res.put(i * 2 + 1, pair[1]);
			}
		}
		{
			res = arr.flat;
		}
		^res
	}

	*fileConvolve {|server, source, impulse, action|

		server.waitForBoot{
			"convolving".postln;
			Buffer.read(server, source, action:{|bufA|
				Buffer.read(server, impulse, action:{|bufB|
					this.doit(server, bufA, bufB, action);
			})})
		}
	}

	*bufConvolve {|server, sourceBuf, impulseBuf, action|
		"convolving".postln;
		this.doit(server, sourceBuf, impulseBuf, action);
	}
}
