/* Copyright 2010 Kenneth Ferland

This file is part of Khazad.

Khazad is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Khazad is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Khazad.  If not, see <http://www.gnu.org/licenses/> */

#ifndef GAME__HEADER
#define GAME__HEADER

#include <stdafx.h>

#include <Singleton.h>

#include <boost/archive/binary_iarchive.hpp>
#include <boost/archive/binary_oarchive.hpp>

class Actor;
class Pawn;
class Tree;
class Map;
class PathManager;
class Geology;
class Timer;
class Settlment;

class MapCoordinates;

class Game
{
DECLARE_SINGLETON_CLASS(Game)

friend class boost::serialization::access;

public:

	~Game();
	bool InitializeGame(uint16_t X, uint16_t Y, const char* Seed);
	bool Run();

    bool BuildMapChunk(int16_t X, int16_t Y, int8_t Width, int8_t Height);

    Pawn* SpawnPawn(MapCoordinates SpawnCoordinates);
    Tree* SpawnTree(MapCoordinates SpawnCoordinates);

    void changeTickRate(int32_t RateChange);
    void setTickRate(uint32_t NewRate);

    uint32_t getTickRate()       { return TickRate; }

    void togglePause()              { Pause = !Pause; }
    void setPause(bool NewState)    { Pause = NewState; }

    bool isZoneing()                 { return Zoneing; }
    void setZoneing(bool NewValue)   { Zoneing = NewValue; }

    Map* getMap()               { return MainMap; }
    Geology* getGeology()       { return MapGeology; }
    PathManager* getPath()      { return Path; }

    float getProgress()             { return ProgressAmount; }
    Timer* getGameTimer()            { return GameTimer; }

protected:

    uint32_t TickRate;     // Simulation Rate;
    bool Pause;
    bool Zoneing;           // User is Designating a Zone

    Timer* GameTimer;

    Map* MainMap;
    Geology* MapGeology;
    PathManager* Path;
    Settlment* TheSettlment;

    float ProgressAmount;

	template<class Archive>
	void serialize(Archive & Arc, const unsigned int version)
	{
		Arc& TickRate;
		Arc& Pause;
		Arc& Zoneing;

		//Arc& GameTimer;

		Arc& MainMap;
		//Arc& MapGeology;
		//Arc& Path;
		//Arc& TheSettlment;
	};
};

#define GAME (Game::GetInstance())

#endif // GAME__HEADER
