package com.dzirbel.kotify.repository

import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.repository.artist.SavedArtistRepository
import com.dzirbel.kotify.repository.playlist.SavedPlaylistRepository
import com.dzirbel.kotify.repository.track.SavedTrackRepository

class FakeSavedAlbumRepository(savedStates: Map<String, Boolean> = emptyMap()) :
    FakeSavedRepository(savedStates), SavedAlbumRepository

class FakeSavedArtistRepository(savedStates: Map<String, Boolean> = emptyMap()) :
    FakeSavedRepository(savedStates), SavedArtistRepository

class FakeSavedPlaylistRepository(savedStates: Map<String, Boolean> = emptyMap()) :
    FakeSavedRepository(savedStates), SavedPlaylistRepository

class FakeSavedTrackRepository(savedStates: Map<String, Boolean> = emptyMap()) :
    FakeSavedRepository(savedStates), SavedTrackRepository
