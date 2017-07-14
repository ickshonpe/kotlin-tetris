package com.ickshonpe.tetris

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import java.util.*

data class P2(val x: Int = 0, val y: Int = 0) {
    fun translate(x: Int = 0, y: Int = 0) = P2(x = this.x + x, y = this.y + y)
}

data class Size2(val width: Int, val height: Int)

data class Shape(val pivot: P2? = P2(), val blocks: List<P2>) {
    fun translate(x: Int = 0, y: Int = 0) =
            Shape(pivot = pivot?.translate(x, y), blocks = blocks.map { it.translate(x, y) })
    fun setPosition(p: P2) =
            if (pivot == null) {
                val tx = p.x - blocks[0].x
                val ty = p.y - blocks[0].y
                Shape(pivot = null, blocks = blocks.map { it.translate(tx, ty) })
            }
            else {
                val tx = p.x - pivot.x
                val ty = p.y - pivot.y
                Shape(pivot = p, blocks = blocks.map { it.translate(tx, ty) })
            }
}

data class Cell(var isFull: Boolean, var color: Color = Color.GOLD)

class Bucket(val size: Size2) {
    private val data = (0..size.height - 1)
                        .map { (0..size.width - 1).map { Cell(isFull = false) }.toMutableList() }
                        .toMutableList()
    operator fun get(p: P2) = data[p.y][p.x]
    operator fun get(x: Int, y: Int) = data[y][x]
    operator fun set(p: P2, value: Cell) {
        data[p.y][p.x] = value
    }
    fun dropLine(line: Int) {
        data.removeAt(line)
        data += (0..size.width - 1).map { Cell(isFull = false) }.toMutableList()
    }
}

class TetrisGame: Game() {
    var highscore = 0
    override fun create() {
        setScreen(TetrisScreen(this))
    }
}

class TetrisScreen(val game: TetrisGame): Screen, InputProcessor {
    val spriteBatch = SpriteBatch()
    val shapeRenderer = ShapeRenderer()
    val font = BitmapFont()
    val random = Random()
    val bucketSize = Size2(width = 10, height = 20)
    val cellPixelSize = Size2(width = 32, height = 32)
    val shapes = listOf(
            Shape(blocks = listOf(P2(x = -2), P2(x = -1), P2(), P2(x = 1))),
            Shape(blocks = listOf(P2(x = -1), P2(), P2(x = 1), P2(x = 1, y = 1))),
            Shape(blocks = listOf(P2(x = -1), P2(), P2(x = 1), P2(x = 1, y = -1))),
            Shape(pivot = null, blocks = listOf(P2(x = -1), P2(), P2(x = -1, y = 1), P2(y = 1))),
            Shape(blocks = listOf(P2(x = -1), P2(), P2(x = 1), P2(y = 1))),
            Shape(blocks = listOf(P2(x = -1), P2(), P2(y = 1), P2(x = 1, y = 1))),
            Shape(blocks = listOf(P2(), P2(x = 1), P2(y = 1), P2(x = -1, y = 1))))
    val spawnPoint = P2(x = 4, y = 20)
    val bucket = Bucket(bucketSize)
    val dropColor = Color.ROYAL
    val fastDropColor = Color.RED
    val backColor = Color.WHITE
    val wallColor = Color.GOLD
    var shapeCount = 0
    var level = 0
    var score = 0
    var currentShape: Shape? = null
    var nextShape: Shape = shapes[random.nextInt(shapes.size)].setPosition(spawnPoint)
    val moveCoolDownTime = 0.1f
    val rotateCoolDownTime = 0.2f
    var rotateCoolDown = 0f
    var moveCoolDown = 0f
    var timeUntilNextDrop = 1f
    var fastDrop = false
    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
    override fun mouseMoved(screenX: Int, screenY: Int): Boolean = false
    override fun keyTyped(character: Char): Boolean = false
    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
    override fun scrolled(amount: Int): Boolean = false
    override fun keyUp(keycode: Int): Boolean = false
    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false
    override fun keyDown(keycode: Int): Boolean = false
    override fun pause() = Unit
    override fun resize(width: Int, height: Int) = Unit
    override fun hide() = Unit
    override fun render(timeDelta: Float) {
        val lines = checkForLines(bucket)
        if (lines.isNotEmpty()) {
            val scorePerLine = bucket.size.width * (level + 1)
            val multiplier = listOf(1, 2, 4, 8) [lines.size - 1]
            score += lines.size * scorePerLine * multiplier
        }
        lines.forEach { bucket.dropLine(it) }
        moveCoolDown -= timeDelta
        rotateCoolDown -= timeDelta
        fastDrop = Gdx.input.isKeyPressed(Input.Keys.DOWN)
        val dropSpeed = timeDelta * (1f + Math.log(2 * level + 1.0).toFloat())
        timeUntilNextDrop =
                if (fastDrop)
                    timeUntilNextDrop - Math.max(0.4f, dropSpeed)
                else
                    timeUntilNextDrop - dropSpeed
        if (currentShape == null) {
            currentShape = nextShape
            nextShape = shapes[random.nextInt(shapes.size)].setPosition(spawnPoint)
            shapeCount++;
            if (shapeCount == 10) {
                level += 1
                shapeCount = 0
            }
        }
        else {
            if (moveCoolDown <= 0f) {
                if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                    if (minX(currentShape!!) > 0) {
                        currentShape = currentShape!!.translate(x = -1)
                        if (collisionCheck(currentShape!!, bucket)) {
                            currentShape = currentShape!!.translate(x = 1)
                        }
                        else {
                            moveCoolDown = moveCoolDownTime
                        }
                    }
                }
                else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                    if (maxX(currentShape!!) < bucketSize.width - 1) {
                        currentShape = currentShape!!.translate(x = 1)
                        if (collisionCheck(currentShape!!, bucket)) {
                            currentShape = currentShape!!.translate(x = -1)
                        }
                        else {
                            moveCoolDown = moveCoolDownTime
                        }
                    }
                }
            }
            if (rotateCoolDown <= 0f) {
                if (Gdx.input.isKeyPressed(Input.Keys.Z)) {
                    var nextShape = rotateLeft(currentShape!!)
                    if (minX(nextShape) < 0) {
                        nextShape = nextShape.translate(x = -minX(nextShape))
                    }
                    if (maxX(nextShape) >= bucket.size.width) {
                        nextShape = nextShape.translate(x = bucket.size.width - maxX(nextShape) - 1)
                    }
                    if (!collisionCheck(nextShape, bucket)) {
                        currentShape = nextShape
                        rotateCoolDown = rotateCoolDownTime
                    }
                }
                else if (Gdx.input.isKeyPressed(Input.Keys.X)) {
                    var nextShape = rotateRight(currentShape!!)
                    if (minX(nextShape) < 0) {
                        nextShape = nextShape.translate(x = -minX(nextShape))
                    }
                    if (maxX(nextShape) >= bucket.size.width) {
                        nextShape = nextShape.translate(x = bucket.size.width - maxX(nextShape) - 1)
                    }
                    if (!collisionCheck(nextShape, bucket)) {
                        currentShape = nextShape
                        rotateCoolDown = rotateCoolDownTime
                    }
                }
            }
        }
        if (timeUntilNextDrop <= 0) {
            currentShape = currentShape!!.translate(y = -1)
            timeUntilNextDrop = 1f
            if (collisionCheck(currentShape!!, bucket)) {
                if (maxY(currentShape!!) < bucketSize.height - 1) {
                    currentShape = currentShape!!.translate(y = 1)
                    currentShape!!.blocks.forEach {
                        bucket[it].isFull = true
                        bucket[it].color = wallColor
                    }
                    score += level + 1
                    currentShape = null
                }
                else {
                    game.highscore = if (score > game.highscore) score else game.highscore
                    game.screen = TetrisScreen(game)
                }
            }
        }
        val screenOffset = (Gdx.graphics.width - (cellPixelSize.width * bucketSize.width)) / 2f
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = backColor
        shapeRenderer.rect(screenOffset, screenOffset, (cellPixelSize.width * bucketSize.width).toFloat(), (cellPixelSize.height * bucketSize.height).toFloat())
        drawBucket(shapeRenderer, bucket, screenOffset, screenOffset, cellPixelSize)
        if (currentShape != null) {
            drawShape(shapeRenderer, currentShape!!, if (fastDrop) fastDropColor else dropColor, cellPixelSize, screenOffset)
        }
        drawShape(shapeRenderer, nextShape.setPosition(P2(x = (bucketSize.width / 2) + 2, y = bucketSize.height + 1)), if (fastDrop) fastDropColor else dropColor, cellPixelSize, screenOffset)
        shapeRenderer.end()
        spriteBatch.begin()
        val textHeight = 20f
        font.draw(spriteBatch, "Level: " + level, screenOffset, Gdx.graphics.height - textHeight)
        font.draw(spriteBatch, "Score: " + score, screenOffset, Gdx.graphics.height - 2 * textHeight)
        font.draw(spriteBatch, "HighScore: " + game.highscore, screenOffset, Gdx.graphics.height - 3 * textHeight)
        spriteBatch.end()
    }
    override fun resume() = Unit
    override fun dispose() = Unit
    override fun show() = Unit
}

fun rotateRight(shape: Shape) =
        if (shape.pivot != null)
            shape.copy(blocks = shape.blocks.map { rotateRight(shape.pivot, it) })
        else
            shape
fun rotateLeft(shape: Shape) =
        if (shape.pivot != null)
            shape.copy(blocks = shape.blocks.map { rotateLeft(shape.pivot, it) })
        else
            shape
fun rotateLeft(pivot: P2, target: P2): P2 {
    val x = target.x - pivot.x
    val y = target.y - pivot.y
    return P2(-y + pivot.x, x + pivot.y)
}
fun rotateRight(pivot: P2, target: P2): P2 {
    val x = target.x - pivot.x
    val y = target.y - pivot.y
    return P2(y + pivot.x, -x + pivot.y)
}
fun checkForLines(bucket: Bucket): List<Int> {
    var lines = emptyList<Int>()
    (bucket.size.height - 1 downTo 0).forEach { y ->
        if ((0..bucket.size.width - 1).all { x -> bucket[x, y].isFull }) {
            lines += y
        }
    }
    return lines
}
fun drawShape(shapeRenderer: ShapeRenderer, shape: Shape, color: Color, cellPixelSize: Size2, offset: Float) {
    shape.blocks.forEach { p ->
        val x = p.x * cellPixelSize.width + offset
        val y = p.y * cellPixelSize.height + offset
        drawBlock(shapeRenderer, x, y, cellPixelSize.width.toFloat(), cellPixelSize.height.toFloat(), color)
    }
}
fun drawBlock(shapeRenderer: ShapeRenderer, x: Float, y: Float, width: Float, height: Float, color: Color) {
    shapeRenderer.color = Color(color.r * 0.7f, color.g * 0.7f, color.b * 0.7f, color.a * 0.7f)
    shapeRenderer.rect(x, y, width, height)
    shapeRenderer.color = color
    shapeRenderer.rect(x + (width * 0.1f), y + (height * 0.1f), width * 0.8f, height * 0.8f)
}
fun drawBucket(shapeRenderer: ShapeRenderer, bucket: Bucket, x: Float, y: Float, cellSize: Size2) {
    for (i in 0..bucket.size.width - 1) {
        for (j in 0..bucket.size.height - 1) {
            val cell = bucket[i, j]
            if (cell.isFull) {
                drawBlock(shapeRenderer, i * cellSize.width + x, j * cellSize.height + y, cellSize.width.toFloat(), cellSize.height.toFloat(), cell.color)
            }
        }
    }
}
fun minX(shape: Shape): Int = shape.blocks.map { it.x }.min()!!
fun maxX(shape: Shape): Int = shape.blocks.map { it.x }.max()!!
fun minY(shape: Shape): Int = shape.blocks.map { it.y }.min()!!
fun maxY(shape: Shape): Int = shape.blocks.map { it.y }.max()!!
fun collisionCheck(shape: Shape, bucket: Bucket) =
        minY(shape) < 0 ||
            shape.blocks.any { it.y < bucket.size.height && bucket[it].isFull }
