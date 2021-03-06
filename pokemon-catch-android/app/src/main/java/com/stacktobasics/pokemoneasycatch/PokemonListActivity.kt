package com.stacktobasics.pokemoneasycatch

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.max
import kotlin.random.Random


class PokemonListActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private lateinit var gridview: GridView

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pokemon_list)
        drawPokemonGridView()
    }

    private fun drawPokemonGridView() {
        // get list of pokemon to show
        // need to remove pokemon not in owned games if hideUnobtainablePokemon is true
        // need to remove pokemon in ownedPokemon list if hideOwnedPokemon is true
        val pokemonToShow = mutableListOf<Pokemon>()
        gridview = findViewById(R.id.gridview)
        Store.allPokemon.forEach { pokemon ->

            var shouldHideBecauseOwned = false
            if (Store.filterOptions.hideOwnedPokemon) {
                if (Store.user.ownedPokemon.contains(pokemon)) shouldHideBecauseOwned = true
            }

            var shouldHideBecauseUnobtainable = false
            if (Store.filterOptions.hideUnobtainablePokemon) {
                shouldHideBecauseUnobtainable = !pokemonCanBeObtained(pokemon)
            }

            val shouldHideBecauseCannotBeCaughtInSelectedGame = !pokemonCanBeCaught(pokemon, Store.filterOptions.selectedGame)

            // if the user has selected to hide owned pokemon and hide unobtainable pokemon, then only
            // show pokemon if it is both not owned and can be obtained
            val shouldHide = (Store.filterOptions.hideOwnedPokemon && shouldHideBecauseOwned) ||
                    (Store.filterOptions.hideUnobtainablePokemon && shouldHideBecauseUnobtainable) ||
                    shouldHideBecauseCannotBeCaughtInSelectedGame

            val matchesSearchCriteria : Boolean = if(Store.search.isBlank()) {
                true
            } else {
                pokemon.name.contains(Store.search, true)
            }

            if (!shouldHide && matchesSearchCriteria) pokemonToShow.add(pokemon)
        }


        val sortedPokemon = when (Store.sort) {
            SortOptions.NAME_ASC -> pokemonToShow.sortedBy { it.name }
            SortOptions.NAME_DESC -> pokemonToShow.sortedByDescending { it.name }
            SortOptions.NUMBER_ASC -> pokemonToShow.sortedBy { it.pokedexNumber }
            SortOptions.NUMBER_DESC -> pokemonToShow.sortedByDescending { it.pokedexNumber }
            else -> pokemonToShow
        }

        gridview.adapter = PokemonAdapter(
            this,
            sortedPokemon
        ) { pokedexNumber  : Int  ->
            val activityIntent = Intent(this, DescriptionActivity::class.java)
            activityIntent.putExtra("pokedex_number", pokedexNumber)
            startActivity(activityIntent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.list_view_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if(query == null) Store.search = ""
        else Store.search = query
        drawPokemonGridView()
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when(item.itemId) {
            R.id.action_filter -> {
                val activityIntent = Intent(this, FilterActivity::class.java)
                startActivity(activityIntent)
                true
            }
            R.id.action_name_asc_dropdown -> {
                Store.sort = SortOptions.NAME_ASC
                drawPokemonGridView()
                true
            }
            R.id.action_name_desc_dropdown -> {
                Store.sort = SortOptions.NAME_DESC
                drawPokemonGridView()
                true
            }
            R.id.action_number_asc_dropdown -> {
                Store.sort = SortOptions.NUMBER_ASC
                drawPokemonGridView()
                true
            }
            R.id.action_number_desc_dropdown -> {
                Store.sort = SortOptions.NUMBER_DESC
                drawPokemonGridView()
                true
            }
            R.id.action_settings -> {
                val activityIntent = Intent(this, SettingsActivity::class.java)
                startActivity(activityIntent)
                true
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

class PokemonAdapter(val context: Context, val pokemon: List<Pokemon>, val startActivityFunc : (Int) -> Unit) : BaseAdapter() {
    // 2
    override fun getCount(): Int {
        return pokemon.size
    }

    // 3
    override fun getItemId(position: Int): Long {
        return 0
    }

    // 4
    override fun getItem(position: Int): Any? {
        return null
    }

    // 5
    @SuppressLint("ResourceType")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val c = ConstraintLayout(context)

        val imageView = ImageView(context)
        val pokemon = pokemon[position]
        Picasso.get().load("${RestClient.url}images/list/${pokemon.pokedexNumber}.png").into(imageView)

        val set = ConstraintSet()
        imageView.id = 100
        imageView.adjustViewBounds = true
        imageView.layoutParams = ViewGroup.LayoutParams(200, 200)
        val greyscale = ColorMatrix()
        greyscale.setSaturation(0f) //0 means greyscale

        val originalSaturation = ColorMatrix()

        val ownsPokemon = Store.user.ownedPokemon.contains(pokemon)
        val canBeCaught = pokemonCanBeCaught(pokemon, Store.user.ownedGames)
        if(!ownsPokemon) {
            val cf = ColorMatrixColorFilter(greyscale)
            imageView.colorFilter = cf
        }
        c.addView(imageView)

        // calculate whether can be caught
        val canBeCaughtImage = ImageView(context)
        canBeCaughtImage.background = context.resources.getDrawable(R.drawable.ic_pokeball)
        canBeCaughtImage.id = Random.nextInt()
//        canBeCaughtImage.alpha = 0.5f
        val showCanBeCaughtImage = !ownsPokemon && canBeCaught
        if(showCanBeCaughtImage) c.addView(canBeCaughtImage)

        // Calculate whether to show egg
        val canBeBred = pokemonCanBeBred(pokemon)
        val egg = ImageView(context)
        egg.background = context.resources.getDrawable(R.drawable.ic_egg)
        egg.id = Random.nextInt()
//        egg.alpha = 0.5f
        val showEggImage = !ownsPokemon && canBeBred
        if(showEggImage) c.addView(egg)

        // calculate whether other pokemon can be caught and then this bred
        val canBeBredIfOtherPokemonCaughtImage = ImageView(context)
        canBeBredIfOtherPokemonCaughtImage.background = context.resources.getDrawable(R.drawable.ic_pokeball_egg)
        canBeBredIfOtherPokemonCaughtImage.id = Random.nextInt()
//        canBeBredIfOtherPokemonCaughtImage.alpha = 0.5f
        val showCaughtAndBredImage = !ownsPokemon && !canBeBred && canBeBredIfOtherPokemonCaught(pokemon)
        if(showCaughtAndBredImage) c.addView(canBeBredIfOtherPokemonCaughtImage)

        // can't be caught
/*        val cross = ImageView(context)
        cross.background = context.resources.getDrawable(R.drawable.ic_close_black_24dp)
        cross.id = Random.nextInt()
        if(!ownsPokemon && !canBeCaught && !canBeBred) rl.addView(cross)*/

        val hasBestCatchRate = !ownsPokemon && pokemonHasBestCatchRateInSelectedGameOutOfOwnedGames(pokemon);
        val star = ImageView(context)
        star.background = context.resources.getDrawable(R.drawable.ic_star)
        star.id = Random.nextInt()
        if(hasBestCatchRate) c.addView(star)

        val pokemonNameTextView = TextView(context)
        pokemonNameTextView.text = pokemon.name
        pokemonNameTextView.id = 150
        c.addView(pokemonNameTextView)
        set.clone(c)

        if(showCanBeCaughtImage) set.connect(canBeCaughtImage.id, ConstraintSet.START,
            ConstraintSet.PARENT_ID, ConstraintSet.START, 8)
        if(showEggImage) set.connect(egg.id, ConstraintSet.END,
            ConstraintSet.PARENT_ID, ConstraintSet.END, 8)
        if(showCaughtAndBredImage) {
            set.connect(canBeBredIfOtherPokemonCaughtImage.id, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START, 8)
            set.connect(canBeBredIfOtherPokemonCaughtImage.id, ConstraintSet.BOTTOM,
                imageView.id, ConstraintSet.BOTTOM, 0)
        }
        if(hasBestCatchRate) {
            set.connect(star.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 8)
            set.connect(star.id, ConstraintSet.BOTTOM, imageView.id, ConstraintSet.BOTTOM, 8)
        }


        set.connect(pokemonNameTextView.getId(), ConstraintSet.LEFT,
            ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0)
        set.connect(pokemonNameTextView.getId(), ConstraintSet.RIGHT,
            ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0)
        set.connect(pokemonNameTextView.getId(), ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)

        set.connect(imageView.id, ConstraintSet.TOP,
            ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        set.connect(imageView.id, ConstraintSet.LEFT,
            ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0)
        set.connect(imageView.id, ConstraintSet.RIGHT,
            ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0)
        set.connect(imageView.id, ConstraintSet.BOTTOM,
            pokemonNameTextView.getId(), ConstraintSet.TOP)

        val value = object : View.OnTouchListener {

            private val gestureDetector = GestureDetector(context, object :
                GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    var message = "Added ${pokemon.name} to collection"
                    if(Store.user.ownedPokemon.contains(pokemon)) {
                        // TODO: set user id properly
                        RestClient.backendAPI.removePokemon("1", pokemon.pokedexNumber).enqueue(object :
                            Callback<User> {
                            override fun onResponse(call: Call<User>, response: Response<User>) {
                                if (response.isSuccessful) {
                                    Store.user.ownedPokemon.remove(pokemon)
                                    this@PokemonAdapter.notifyDataSetChanged()
                                    message = "Removed ${pokemon.name} from collection"
                                    Snackbar.make(
                                        c,
                                        message,
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                    imageView.colorFilter = ColorMatrixColorFilter(greyscale)
                                }
                            }

                            override fun onFailure(call: Call<User>, t: Throwable) {
                                println("Exception when removing pokemon: " + t.message)
                                throw t
                            }
                        })

                    }
                    else {
                        // TODO: set user id properly
                        RestClient.backendAPI.addPokemon("1", pokemon.pokedexNumber).enqueue(object :
                            Callback<User> {
                            override fun onResponse(call: Call<User>, response: Response<User>) {
                                if (response.isSuccessful) {
                                    Store.user.ownedPokemon.add(pokemon)
                                    this@PokemonAdapter.notifyDataSetChanged()
                                    message = "Added ${pokemon.name} from collection"
                                    Snackbar.make(
                                        c,
                                        message,
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                    imageView.colorFilter = ColorMatrixColorFilter(originalSaturation)
                                }
                            }

                            override fun onFailure(call: Call<User>, t: Throwable) {
                                println("Exception when adding pokemon: " + t.message)
                                throw t
                            }
                        })
                    }

                    return super.onDoubleTap(e)
                }

                override fun onLongPress(e: MotionEvent?) {
                    // not implemented yet
                    /*val snackbar = Snackbar.make(
                        c,
                        "Should start ",
                        Snackbar.LENGTH_SHORT
                    )
                    snackbar.show()*/
                    return super.onLongPress(e)
                }

                override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                    startActivityFunc(pokemon.pokedexNumber)
                    return super.onSingleTapConfirmed(e)
                }

            })

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                Log.d(
                    "TEST",
                    "Raw event: " + event?.getAction() + ", (" + event?.getRawX() + ", " + event?.getRawY() + ")"
                );
                gestureDetector.onTouchEvent(event);
                return true;
            }
        }

        c.setOnTouchListener(value)

        set.applyTo(c)
        return c
    }
}

private fun canBeBredIfOtherPokemonCaught(pokemon: Pokemon): Boolean {
    val evolutionChain =
        Store.allEvolutionChains.find { pokemon.evolutionChainId == it.id } ?: return false

    val allPokemonInChain = evolutionChain.allPokemonInChain.toMutableList()
    allPokemonInChain.remove(pokemon.pokedexNumber)
    allPokemonInChain.find { pId ->
        val foundPokemon = Store.allPokemon.find { p -> p.pokedexNumber == pId } ?: return false
        pokemonCanBeCaught(foundPokemon, Store.user.ownedGames) } ?: return false
    return true
}

fun pokemonCanBeBred(pokemon: Pokemon): Boolean {
    // get evolution chain for pokemon
    // if any of the pokemon in that chain exist in owned pokemon, then show egg
    val ownedPokemonIds = Store.user.ownedPokemon.map { p -> p.pokedexNumber }

    val evolutionChain =
        Store.allEvolutionChains.find { pokemon.evolutionChainId == it.id } ?: return false

    for (pokedexId in evolutionChain.allPokemonInChain) {
        if(ownedPokemonIds.contains(pokedexId)) return true
    }
    return false
}

fun pokemonCanBeObtained(pokemon : Pokemon) : Boolean {
    return pokemonCanBeCaught(pokemon, Store.user.ownedGames) ||
            pokemonCanBeBred(pokemon) ||
            canBeBredIfOtherPokemonCaught(pokemon)
}

fun pokemonCanBeCaught(pokemon : Pokemon, games : Collection<Game>) : Boolean {
    if(games == Store.allGames) return true
    val gameIds: List<Int> = games.map { game -> game.id }
    for (encounteredGame in pokemon.encounterDetails.encounters.map { encounter -> encounter.location.gameId }) {
        if (gameIds.contains(encounteredGame)) return true
    }
    return false
}

fun pokemonHasBestCatchRateInSelectedGameOutOfOwnedGames(pokemon: Pokemon) : Boolean {
    val selectedGame = Store.filterOptions.selectedGame
    if(selectedGame !is Game) return false
    if(!Store.user.ownedGames.contains(selectedGame)) return false

    var bestCatchRateForSelectedGame = -1
    for (encounter in pokemon.encounterDetails.encounters) {
        if(selectedGame.id != encounter.location.gameId) continue
        bestCatchRateForSelectedGame = max(bestCatchRateForSelectedGame, encounter.catchRate)
    }
    if(bestCatchRateForSelectedGame == -1) return false

    val ownedGames = Store.user.ownedGames.map { g -> g.id }
    for (encounter in pokemon.encounterDetails.encounters) {
        if(!ownedGames.contains(encounter.location.gameId)) continue
        if(encounter.catchRate > bestCatchRateForSelectedGame) return false
    }
    return true
}

fun pokemonCanBeCaught(pokemon : Pokemon, game : Game?) : Boolean {
    if(game == null) return true
    for (encounteredGame in pokemon.encounterDetails.encounters.map { encounter -> encounter.location.gameId }) {
        if (encounteredGame == game.id) return true
    }
    return false
}
